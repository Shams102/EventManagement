import React, { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createThread, fetchInbox, markDeliveryRead, muteDelivery } from '../lib/api'
import { showToast } from '../lib/toast'

export default function NotificationsPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('ALL')
  const [query, setQuery] = useState('')

  const load = async () => {
    setLoading(true)
    try {
      const res = await fetchInbox()
      setItems(res.data || [])
    } catch {
      setItems([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  useEffect(() => {
    window.dispatchEvent(new Event('notifications-updated'))
  }, [items])

  useEffect(() => {
    if (loading) return
    const unread = (items || []).filter(i => !i.read)
    if (unread.length === 0) return
    Promise.all(unread.map(i => markDeliveryRead(i.deliveryId).catch(() => {}))).then(() => {
      setItems(prev => prev.map(x => ({ ...x, read: true })))
      window.dispatchEvent(new Event('notifications-updated'))
    })
  }, [loading])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (items || []).filter(n => {
      if (filter === 'GLOBAL' && n.origin !== 'GLOBAL') return false
      if (filter === 'EVENT' && n.origin !== 'EVENT') return false
      if (!q) return true
      return String(n.title || '').toLowerCase().includes(q) || String(n.message || '').toLowerCase().includes(q)
    })
  }, [items, filter, query])

  const openThread = async (item) => {
    if (!item || !item.threadEnabled) {
      showToast({ message: 'Discussion is not enabled for this notification', type: 'error' })
      return
    }
    try {
      const notificationId = Number(item.id)
      const res = await createThread({ notificationId, title: item.title })
      const threadId = res.data.threadId
      navigate(`/notifications/threads/${threadId}`)
    } catch (e) {
      console.error(e)
      showToast({ message: 'Failed to open chat', type: 'error' })
    }
  }

  const onMarkRead = async (item) => {
    if (!item?.deliveryId) return
    try {
      await markDeliveryRead(item.deliveryId)
      setItems(prev => prev.map(i => i.deliveryId === item.deliveryId ? { ...i, read: true } : i))
    } catch (e) {
      console.error(e)
    }
  }

  const onMute = async (item) => {
    if (!item?.deliveryId) return
    try {
      await muteDelivery(item.deliveryId, !item.muted)
      setItems(prev => prev.map(i => i.deliveryId === item.deliveryId ? { ...i, muted: !i.muted } : i))
    } catch (e) {
      console.error(e)
    }
  }

  const unreadCount = (items || []).filter(i => !i.read).length

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-[#E5E7EB]">Notifications</h1>
          <div className="text-sm text-[#9CA3AF]">Unread: {unreadCount}</div>
        </div>
        <div className="flex gap-2">
          <button className="btn btn-secondary btn-sm" onClick={load} disabled={loading}>Refresh</button>
          <button className="btn btn-secondary btn-sm" onClick={async () => {
            const unread = (items || []).filter(i => !i.read)
            await Promise.all(unread.map(i => markDeliveryRead(i.deliveryId).catch(() => {})))
            setItems(prev => prev.map(x => ({ ...x, read: true })))
            window.dispatchEvent(new Event('notifications-updated'))
          }} disabled={loading || unreadCount === 0}>Mark all read</button>
        </div>
      </div>

      <div className="card mb-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex gap-2">
            <button className={`btn btn-sm ${filter === 'ALL' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setFilter('ALL')}>All</button>
            <button className={`btn btn-sm ${filter === 'GLOBAL' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setFilter('GLOBAL')}>Global</button>
            <button className={`btn btn-sm ${filter === 'EVENT' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setFilter('EVENT')}>Event</button>
          </div>
          <input className="form-input sm:w-80" value={query} onChange={e => setQuery(e.target.value)} placeholder="Search title or message" aria-label="Search notifications" />
        </div>
      </div>

      {loading ? (
        <div className="text-sm text-[#9CA3AF]">Loading...</div>
      ) : filtered.length === 0 ? (
        <div className="text-sm text-[#9CA3AF]">No notifications</div>
      ) : (
        <div className="space-y-3">
          {filtered.map(item => (
            <div key={item.deliveryId} className={`card card-compact interactive ${!item.read ? 'border border-indigo-100' : ''}`}>
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <div className="font-semibold text-[#E5E7EB]">{item.title}</div>
                    {!item.read && <span className="text-xs text-indigo-300">New</span>}
                    {item.muted && <span className="text-xs text-[#9CA3AF]">Muted</span>}
                  </div>
                  <div className="text-xs text-[#9CA3AF] mt-1">
                    {item.origin === 'EVENT' ? 'Event' : 'Global'} • {new Date(item.createdAt).toLocaleString()}
                  </div>
                  <div className="text-sm text-[#D1D5DB] mt-2 whitespace-pre-line">{item.message}</div>
                </div>
                <div className="flex flex-col gap-2 items-end">
                  {item.threadEnabled && (
                    <button className="btn btn-secondary btn-sm" type="button" onClick={(e) => { e.preventDefault(); e.stopPropagation(); openThread(item) }}>Open chat</button>
                  )}
                  <button className="btn btn-ghost btn-sm" onClick={() => onMarkRead(item)} disabled={item.read}>Mark read</button>
                  <button className="btn btn-ghost btn-sm" onClick={() => onMute(item)}>{item.muted ? 'Unmute' : 'Mute'}</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

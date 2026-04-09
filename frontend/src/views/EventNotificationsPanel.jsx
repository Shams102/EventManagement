import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchEventNotifications, createThread, postEventNotification } from '../lib/api'

export default function EventNotificationsPanel({ eventId, canView, canPost }) {
  const [items, setItems] = useState([])
  const navigate = useNavigate()

  // composer state (event owners / admin)
  const [title, setTitle] = useState('')
  const [messageText, setMessageText] = useState('')
  const [urgency, setUrgency] = useState('NORMAL')
  const [threadEnabledFlag, setThreadEnabledFlag] = useState(false)
  const [posting, setPosting] = useState(false)
  const [postMsg, setPostMsg] = useState('')

  useEffect(() => {
    if (!canView) return
    fetchEventNotifications(eventId).then(res => setItems(res.data || [])).catch(() => setItems([]))
  }, [eventId, canView])

  const openDiscussion = async (item) => {
    try {
      const res = await createThread({ notificationId: item.id, title: item.title })
      const threadId = res.data.threadId
      navigate(`/notifications/threads/${threadId}`)
    } catch (err) { console.error(err) }
  }

  const postNotification = async () => {
    if (!title.trim() || !messageText.trim()) {
      setPostMsg('Title and message are required')
      return
    }
    setPosting(true)
    setPostMsg('')
    try {
      await postEventNotification(eventId, { title: title.trim(), message: messageText.trim(), urgency, threadEnabled: threadEnabledFlag })
      setTitle('')
      setMessageText('')
      setThreadEnabledFlag(false)
      setPostMsg('Posted')
      const res = await fetchEventNotifications(eventId)
      setItems(res.data || [])
      // UX feedback
      const toastEvt = new CustomEvent('app-toast', { detail: { id: Date.now(), message: 'Notification posted', type: 'success', timeout: 3500 } })
      window.dispatchEvent(toastEvt)
    } catch (err) {
      console.error(err)
      setPostMsg('Failed to post notification')
      const toastEvt = new CustomEvent('app-toast', { detail: { id: Date.now(), message: 'Failed to post notification', type: 'error', timeout: 3500 } })
      window.dispatchEvent(toastEvt)
    } finally {
      setPosting(false)
    }
  }

  if (!canView) return null
  return (
    <div className="card mt-6">
      <h3 className="text-lg font-semibold text-[#E5E7EB] mb-4">Event Notifications</h3>
      {canPost && (
        <div className="mb-4 card card-compact" aria-live="polite">
          <div className="space-y-2">
            <input value={title} onChange={e => setTitle(e.target.value)} placeholder="Notification title" className="form-input" aria-label="Notification title" aria-invalid={!title.trim()} />
            {!title.trim() && <div className="text-xs text-red-400">Title is required</div>}
            <textarea
              value={messageText}
              onChange={e => setMessageText(e.target.value)}
              placeholder="Message (visible to recipients)"
              className="form-input w-full min-h-[120px] p-3 resize-y"
              rows={5}
              aria-label="Notification message"
              aria-invalid={!messageText.trim()}
            />
            {!messageText.trim() && <div className="text-xs text-red-400">Message is required</div>}
            <div className="flex items-center gap-3">
              <select value={urgency} onChange={e => setUrgency(e.target.value)} className="form-select" aria-label="Urgency">
                <option value="LOW">Low</option>
                <option value="NORMAL">Normal</option>
                <option value="HIGH">High</option>
              </select>
              <label className="flex items-center gap-2 text-sm text-[#9CA3AF]">
                <input type="checkbox" checked={threadEnabledFlag} onChange={e => setThreadEnabledFlag(e.target.checked)} />
                Enable discussion
              </label>
              <button className="btn btn-primary btn-sm" onClick={postNotification} disabled={posting || !title.trim() || !messageText.trim()} aria-disabled={posting || !title.trim() || !messageText.trim()}>{posting ? 'Posting...' : 'Post'}</button>
              {postMsg && <div className="text-sm text-[#9CA3AF] ml-2">{postMsg}</div>}
            </div>
          </div>
        </div>
      )}
      {items.length === 0 && <div className="text-sm text-[#9CA3AF]">No notifications for this event.</div>}
      <div className="space-y-3">
        {items.map(it => (
          <div key={it.deliveryId} className="p-3 border border-[#1F2937] rounded-lg bg-[#0F172A] fade-in-up interactive">
            <div className="flex justify-between items-start">
              <div>
                <div className="font-semibold text-[#E5E7EB]">{it.title}</div>
                <div className="text-sm text-[#D1D5DB] mt-1">{it.message}</div>
                <div className="text-xs text-[#9CA3AF] mt-2">{new Date(it.createdAt).toLocaleString()}</div>
              </div>
              <div className="flex flex-col gap-2 items-end">
                {it.threadEnabled && <button type="button" onClick={() => openDiscussion(it)} className="btn btn-ghost btn-sm">Discuss</button>}
                {!it.read && <span className="text-xs text-indigo-300">New</span>}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

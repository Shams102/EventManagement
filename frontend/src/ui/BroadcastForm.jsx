import React, { useState } from 'react'
import { broadcastNotification } from '../lib/api'
import { showToast } from '../lib/toast'

export default function BroadcastForm({ onSent }) {
  const [title, setTitle] = useState('')
  const [message, setMessage] = useState('')
  const [urgency, setUrgency] = useState('NORMAL')
  const [threadEnabled, setThreadEnabled] = useState(false)
  const [loading, setLoading] = useState(false)
  const [status, setStatus] = useState('')

  const submit = async (e) => {
    e && e.preventDefault()
    if (!title.trim() || !message.trim()) {
      setStatus('Title and message are required')
      showToast({ message: 'Please provide a title and message', type: 'error' })
      return
    }
    setLoading(true)
    setStatus('')
    try {
      await broadcastNotification({ title: title.trim(), message: message.trim(), urgency, threadEnabled })
      setStatus('Sent')
      showToast({ message: 'Broadcast sent', type: 'success' })
      setTitle('')
      setMessage('')
      setThreadEnabled(false)
      onSent && onSent()
    } catch (err) {
      console.error(err)
      setStatus('Failed to send')
      showToast({ message: 'Failed to send broadcast', type: 'error' })
    } finally { setLoading(false) }
  }

  const isInvalid = !title.trim() || !message.trim()

  return (
    <form onSubmit={submit} className="space-y-3" aria-live="polite">
      <div>
        <label htmlFor="broadcast-title" className="form-label">Title</label>
        <input id="broadcast-title" className={`form-input ${!title.trim() && 'border-red-200'}`} value={title} onChange={e => setTitle(e.target.value)} aria-invalid={!title.trim()} />
        {!title.trim() && <div className="text-xs text-red-600 mt-1">Title is required</div>}
      </div>
      <div>
        <label htmlFor="broadcast-message" className="form-label">Message</label>
        <textarea id="broadcast-message" className={`form-input h-24 ${!message.trim() && 'border-red-200'}`} rows={3} value={message} onChange={e => setMessage(e.target.value)} aria-invalid={!message.trim()} />
        {!message.trim() && <div className="text-xs text-red-600 mt-1">Message is required</div>}
      </div>
      <div className="flex items-center gap-3">
        <select className="form-select" value={urgency} onChange={e => setUrgency(e.target.value)} aria-label="Urgency">
          <option value="LOW">Low</option>
          <option value="NORMAL">Normal</option>
          <option value="HIGH">High</option>
        </select>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={threadEnabled} onChange={e => setThreadEnabled(e.target.checked)} /> Enable discussion
        </label>
        <div className="flex-1 text-right">
          <button type="submit" className="btn btn-primary" disabled={loading || isInvalid}>{loading ? 'Sending...' : 'Send'}</button>
        </div>
      </div>
      {status && <div className={`text-sm ${status === 'Sent' ? 'text-green-700' : 'text-red-600'}`}>{status}</div>}
    </form>
  )
}

import React, { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { fetchThreadMessages, postThreadMessage } from '../lib/api'

export default function ThreadChatPage() {
  const { threadId } = useParams()
  const navigate = useNavigate()
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [text, setText] = useState('')
  const bottomRef = useRef(null)

  const load = async () => {
    setLoading(true)
    try {
      const res = await fetchThreadMessages(threadId)
      setMessages(res.data || [])
    } catch (e) {
      console.error(e)
      setMessages([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [threadId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length])

  const send = async () => {
    const content = text.trim()
    if (!content) return
    setSending(true)
    try {
      await postThreadMessage(threadId, { content })
      setText('')
      await load()
    } catch (e) {
      console.error(e)
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-4 flex items-center justify-between">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/notifications')}>← Back</button>
        <div className="text-sm text-[#9CA3AF]">Thread #{threadId}</div>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="border-b border-[#1F2937] p-4 bg-[#0F172A]">
          <div className="font-semibold text-[#E5E7EB]">Discussion</div>
          <div className="text-xs text-[#9CA3AF]">Ask a question and get replies here.</div>
        </div>

        <div className="p-4 bg-[#111827]" style={{ minHeight: 420, maxHeight: 560, overflowY: 'auto' }}>
          {loading ? (
            <div className="text-sm text-[#9CA3AF]">Loading messages...</div>
          ) : messages.length === 0 ? (
            <div className="text-sm text-[#9CA3AF]">No messages yet. Start the conversation.</div>
          ) : (
            <div className="space-y-3">
              {messages.map(m => (
                <div key={m.id} className="flex">
                  <div className="max-w-[85%] rounded-2xl border border-[#1F2937] bg-[#0F172A] px-3 py-2 shadow-sm">
                    <div className="text-xs text-[#9CA3AF]">{m.author} • {new Date(m.createdAt).toLocaleString()}</div>
                    <div className="text-sm text-[#E5E7EB] whitespace-pre-line mt-1">{m.content}</div>
                  </div>
                </div>
              ))}
              <div ref={bottomRef} />
            </div>
          )}
        </div>

        <div className="border-t border-[#1F2937] p-3 bg-[#0F172A]">
          <div className="flex gap-2">
            <input
              className="form-input flex-1"
              placeholder="Type a message..."
              value={text}
              onChange={e => setText(e.target.value)}
              onKeyDown={e => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault()
                  send()
                }
              }}
              aria-label="Message"
              disabled={sending}
            />
            <button className="btn btn-primary" onClick={send} disabled={sending || !text.trim()}>
              {sending ? 'Sending...' : 'Send'}
            </button>
          </div>
          <div className="mt-2 text-xs text-[#9CA3AF]">Press Enter to send.</div>
        </div>
      </div>
    </div>
  )
}

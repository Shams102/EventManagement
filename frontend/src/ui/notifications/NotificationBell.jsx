import React, { useEffect, useState } from 'react'
import { fetchInbox } from '../../lib/api'

export default function NotificationBell({ onOpen, open = false }) {
  const [count, setCount] = useState(0)

  useEffect(() => {
    let mounted = true
    fetchInbox().then(res => {
      if (!mounted) return
      const unread = (res.data || []).filter(n => !n.read).length
      setCount(unread)
    }).catch(() => {})
    return () => { mounted = false }
  }, [])

  return (
    <button
      aria-label="Notifications"
      aria-haspopup="dialog"
      aria-expanded={open}
      onClick={onOpen}
      className={`relative p-2 rounded-full hover:bg-white/5 transition-all duration-200 ease ${count > 0 ? 'animate-bell' : ''}`}
    >
      <svg className="w-6 h-6 text-[#E5E7EB]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 10-12 0v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0a3 3 0 11-6 0h6z" />
      </svg>
      {count > 0 && (
        <span className="absolute -top-1 -right-1 inline-flex items-center justify-center px-2 py-0.5 text-xs font-bold leading-none text-white bg-red-600 rounded-full" role="status" aria-live="polite" aria-label={`${count} unread notifications`}>{count}</span>
      )}
    </button>
  )
}

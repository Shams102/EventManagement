import React, { useEffect, useRef } from 'react'
import BroadcastForm from './BroadcastForm'
import useClickOutside from './useClickOutside'

export default function BroadcastModal({ open, onClose }) {
  const cardRef = useRef(null)

  useClickOutside(cardRef, () => onClose && onClose(), open)

  useEffect(() => {
    if (!open) return
    const onKey = (e) => { if (e.key === 'Escape') onClose && onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="fixed inset-0 z-[1200] flex items-center justify-center bg-black/60 backdrop-blur-sm transition-all duration-200 ease-out">
      <div className="w-full max-w-xl p-4">
        <div ref={cardRef} className="card">
          <div className="flex items-center justify-between mb-4">
            <div className="text-lg font-semibold">Send Global Broadcast</div>
            <button className="btn btn-ghost" onClick={onClose} aria-label="Close broadcast dialog">✕</button>
          </div>
          <BroadcastForm onSent={onClose} />
        </div>
      </div>
    </div>
  )
}

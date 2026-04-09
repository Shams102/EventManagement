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
    <div className="fixed right-5 top-[70px] z-[9999] w-[min(350px,calc(100vw-2.5rem))]">
      <div ref={cardRef} className="card border border-[#1E293B] bg-[#0F172A] shadow-[0_10px_30px_rgba(0,0,0,0.5)]">
        <div className="mb-4 flex items-center justify-between">
          <div className="text-base font-semibold text-[#E5E7EB]">Send Global Broadcast</div>
          <button className="btn btn-ghost btn-sm" onClick={onClose} aria-label="Close broadcast dialog">✕</button>
        </div>
        <BroadcastForm onSent={onClose} />
      </div>
    </div>
  )
}

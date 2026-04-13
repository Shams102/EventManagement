import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../lib/api'

export default function Bookings() {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/api/room-requests/mine')
      .then(async (res) => {
        const list = res.data || []
        const eventIds = Array.from(new Set(
          list.map(b => Number(b.eventId)).filter(n => !Number.isNaN(n) && n > 0)
        ))
        let allocations = {}
        if (eventIds.length > 0) {
          const pairs = await Promise.allSettled(eventIds.map(async (eventId) => {
            const a = await api.get(`/api/events/${eventId}/room-allocations`)
            return [eventId, a.data]
          }))
          pairs.forEach(p => {
            if (p.status === 'fulfilled' && Array.isArray(p.value)) {
              allocations[Number(p.value[0])] = p.value[1]
            }
          })
        }
        const withAlloc = list.map(b => ({ ...b, _allocation: allocations[Number(b.eventId)] || null }))
        setBookings(withAlloc)
      })
      .catch(() => setError('Failed to load bookings'))
      .finally(() => setLoading(false))
  }, [])

  const renderAllocated = (booking) => {
    const rawSlots = Array.isArray(booking?._allocation?.slots) ? booking._allocation.slots : []
    // Sort slots by date to guarantee display order
    const slots = [...rawSlots].sort((a, b) => {
      const da = a?.date || ''; const db = b?.date || ''
      return da < db ? -1 : da > db ? 1 : 0
    })
    if (slots.length === 0) return booking.allocatedRoom || 'TBD'
    if (slots.length === 1) {
      const s = slots[0]
      return s?.allocated ? (s?.roomName || s?.room || 'TBD') : 'TBD'
    }
    return (
      <div className="mt-1 space-y-1">
        {slots.map((s, i) => (
          <div key={`${booking.id}-slot-${i}`}>
            Day {(s?.dayIndex != null ? s.dayIndex + 1 : i + 1)} → {s?.allocated ? (s?.roomName || s?.room || 'TBD') : 'TBD'}
          </div>
        ))}
      </div>
    )
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'confirmed': return 'bg-emerald-900/40 text-emerald-300 border border-emerald-700/40'
      case 'pending': return 'bg-amber-900/40 text-amber-300 border border-amber-700/40'
      case 'completed': return 'bg-slate-800 text-slate-300 border border-slate-700'
      default: return 'bg-slate-800 text-slate-300 border border-slate-700'
    }
  }

  const stats = (() => {
    const total = bookings.length
    const pending = bookings.filter(b => String(b.status || '').toUpperCase() === 'PENDING').length
    const approved = bookings.filter(b => String(b.status || '').toUpperCase() === 'APPROVED').length
    const confirmed = bookings.filter(b => String(b.status || '').toUpperCase() === 'CONFIRMED').length
    const allocatedRooms = new Set(bookings.map(b => b.allocatedRoom).filter(Boolean))
    let minutes = 0
    for (const b of bookings) {
      if (!b.start || !b.end) continue
      const s = new Date(b.start)
      const e = new Date(b.end)
      const diff = (e.getTime() - s.getTime()) / 60000
      if (Number.isFinite(diff) && diff > 0) minutes += diff
    }
    const hoursBooked = Math.round((minutes / 60) * 10) / 10
    return { total, pending, approved, confirmed, rooms: allocatedRooms.size, hoursBooked }
  })()

  return (
    <div>
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-[#E5E7EB]">My Room Requests</h1>
          <p className="text-sm text-[#9CA3AF]">Track your room booking requests and their approval status</p>
        </div>
        <div className="flex gap-2">
          <Link to="/book-room" className="btn btn-primary">Book a New Room</Link>
        </div>
      </div>

      {error && (
        <div className="alert alert-error mb-6">{error}</div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div className="card">
          <div className="text-sm text-[#9CA3AF]">Total</div>
          <div className="mt-1 text-3xl font-bold text-[#E5E7EB]">{stats.total}</div>
        </div>
        <div className="card">
          <div className="text-sm text-[#9CA3AF]">Pending</div>
          <div className="mt-1 text-3xl font-bold text-[#E5E7EB]">{stats.pending}</div>
        </div>
        <div className="card">
          <div className="text-sm text-[#9CA3AF]">Confirmed</div>
          <div className="mt-1 text-3xl font-bold text-[#E5E7EB]">{stats.confirmed}</div>
        </div>
        <div className="card">
          <div className="text-sm text-[#9CA3AF]">Hours booked</div>
          <div className="mt-1 text-3xl font-bold text-[#E5E7EB]">{stats.hoursBooked}</div>
        </div>
      </div>

      {loading ? (
        <div className="card text-center py-12">
          <div className="spinner mx-auto mb-4"></div>
          <p className="text-[#9CA3AF]">Loading your requests...</p>
        </div>
      ) : bookings.length === 0 ? (
        <div className="card text-center py-12">
          <div className="text-4xl mb-4">🏢</div>
          <h3 className="text-lg font-semibold text-[#E5E7EB] mb-2">No requests yet</h3>
          <p className="text-[#9CA3AF] mb-6">Start by submitting your first room request</p>
          <Link to="/book-room" className="btn btn-primary">
            Book a Room
          </Link>
        </div>
      ) : (
        <div className="space-y-6">
          {bookings.map(booking => (
            <div key={booking.id} className="card" style={{ transition: 'all 0.2s ease' }}>
              <div className="flex items-start justify-between mb-4">
                <div>
                  <h3 className="text-lg font-semibold text-[#E5E7EB]">{booking.eventTitle || 'Meeting'}</h3>
                  <p className="text-[#9CA3AF] text-sm">Allocated: {renderAllocated(booking)}</p>
                </div>
                <span className={`text-xs px-2 py-1 rounded-full ${getStatusColor((booking.status || '').toLowerCase())}`}>
                  {(booking.status || '').toUpperCase()}
                </span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                <div>
                  <div className="text-sm font-medium text-[#E5E7EB]">Date</div>
                  <div className="text-sm text-[#9CA3AF]">
                    {booking.start ? new Date(booking.start).toLocaleDateString() : '-'}
                  </div>
                </div>
                <div>
                  <div className="text-sm font-medium text-[#E5E7EB]">Time</div>
                  <div className="text-sm text-[#9CA3AF]">
                    {booking.start ? (
                      `${new Date(booking.start).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}${booking.end ? ` – ${new Date(booking.end).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}` : ''}`
                    ) : '-'}
                  </div>
                </div>
                <div>
                  <div className="text-sm font-medium text-[#E5E7EB]">Status</div>
                  <div className="text-sm text-[#9CA3AF]">{booking.status}</div>
                </div>
              </div>
              {(booking.pref1 || booking.pref2 || booking.pref3) && (
                <div className="text-xs text-[#9CA3AF]">Preferences: {booking.pref1 || '—'} → {booking.pref2 || '—'} → {booking.pref3 || '—'}</div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

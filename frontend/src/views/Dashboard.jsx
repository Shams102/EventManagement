import React, { useEffect, useState } from 'react'
import { useAuth } from '../lib/AuthContext'
import { showToast } from '../lib/toast'
import api from '../lib/api'
import { useNavigate } from 'react-router-dom'
import Skeleton from '../ui/Skeleton'

export default function Dashboard() {
  const { user, hasRole } = useAuth()
  const [registrations, setRegistrations] = useState([])
  const [createdEvents, setCreatedEvents] = useState([])
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [cancellingId, setCancellingId] = useState(null)
  const [confirmId, setConfirmId] = useState(null)
  const navigate = useNavigate()

  const isGeneralLike = hasRole('GENERAL_USER') || hasRole('CLUB_ASSOCIATE')
  const isCreatorRole = hasRole('ADMIN') || hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE')

  useEffect(() => {
    const calls = []
    // Registrations only matter for general users and club associates
    if (isGeneralLike) {
      calls.push(api.get('/api/registrations/mine').catch(() => ({ key: 'regsLegacy', data: [] })))
      calls.push(api.get('/api/event-registrations/mine').catch(() => ({ key: 'regsUser', data: [] })))
    } else {
      calls.push(Promise.resolve({ key: 'regsLegacy', data: [] }))
      calls.push(Promise.resolve({ key: 'regsUser', data: [] }))
    }
    // Created events for roles that can create
    if (isCreatorRole) {
      calls.push(api.get('/api/events/mine').catch(() => ({ key: 'created', data: [] })))
      calls.push(api.get('/api/room-requests/mine').catch(() => ({ key: 'bookings', data: [] })))
    } else {
      calls.push(Promise.resolve({ key: 'created', data: [] }))
      calls.push(Promise.resolve({ key: 'bookings', data: [] }))
    }

    Promise.all(calls).then(([regsLegacyRes, regsUserRes, createdRes, bookingsRes]) => {
      const legacy = regsLegacyRes.data || []
      const userRegs = regsUserRes.data || []
      const mergedByEventId = new Map()

      for (const r of legacy) {
        if (r && r.eventId != null) mergedByEventId.set(Number(r.eventId), r)
      }
      for (const r of userRegs) {
        if (!r || r.eventId == null) continue
        const eventId = Number(r.eventId)
        if (!mergedByEventId.has(eventId)) {
          mergedByEventId.set(eventId, r)
        }
      }
      setRegistrations(Array.from(mergedByEventId.values()))
      setCreatedEvents(createdRes.data || [])
      setBookings(bookingsRes.data || [])
    }).finally(() => setLoading(false))
  }, [isGeneralLike, isCreatorRole])

  const now = new Date()
  const upcomingRegs = registrations.filter(e => e.startTime && new Date(e.endTime) >= now)
  const pastRegs = registrations.filter(e => e.endTime && new Date(e.endTime) < now)

  const getStatusBadge = (status) => {
    const s = (status || '').toUpperCase()
    if (s === 'CONFIRMED') return 'bg-green-100 text-green-800'
    if (s === 'APPROVED') return 'bg-blue-100 text-blue-800'
    if (s === 'PENDING') return 'bg-yellow-100 text-yellow-800'
    if (s === 'REJECTED') return 'bg-red-100 text-red-800'
    return 'bg-gray-100 text-gray-800'
  }

  const handleCancelEvent = async (eventId) => {
    setCancellingId(eventId)
    try {
      await api.delete(`/api/events/${eventId}`)
      setCreatedEvents(prev => prev.filter(ev => ev.id !== eventId))
      showToast({ message: 'Event cancelled successfully', type: 'success' })
    } catch (err) {
      const msg = err.response?.data || 'Failed to cancel event'
      showToast({ message: msg, type: 'error' })
    } finally {
      setCancellingId(null)
      setConfirmId(null)
    }
  }

  const showRegistered = isGeneralLike
  const showCreatorPanels = isCreatorRole
  const mainGridClass = (showRegistered && showCreatorPanels)
    ? 'grid grid-cols-1 lg:grid-cols-2 gap-6'
    : 'grid grid-cols-1 gap-6'

  return (
    <div>
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-600">Welcome back, {user?.sub}!</p>
        </div>
        <div className="flex gap-2">
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => navigate('/events')}>Browse Events</button>
          {(hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('ADMIN')) && (
            <button type="button" className="btn btn-primary btn-sm" onClick={() => navigate('/book-room')}>Book Room</button>
          )}
          {hasRole('ADMIN') && (
            <button type="button" className="btn btn-secondary btn-sm" onClick={() => navigate('/admin/notifications')}>Broadcast</button>
          )}
        </div>
      </div>

      {/* Quick Stats (role-aware) */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        {isGeneralLike && (
          <div className="card">
            <div className="flex items-start justify-between">
              <div>
                <div className="text-sm text-gray-600">Upcoming Registrations</div>
                <div className="text-3xl font-bold text-gray-900 mt-1">{upcomingRegs.length}</div>
              </div>
              <div className="h-10 w-10 rounded-lg" style={{ background: 'rgba(37,99,235,0.35)' }}>
                <div className="h-10 w-10 flex items-center justify-center text-lg">📌</div>
              </div>
            </div>
          </div>
        )}
        {isCreatorRole && (
          <div className="card">
            <div className="flex items-start justify-between">
              <div>
                <div className="text-sm text-gray-600">Events Created</div>
                <div className="text-3xl font-bold text-gray-900 mt-1">{createdEvents.length}</div>
              </div>
              <div className="h-10 w-10 rounded-lg" style={{ background: 'rgba(16,185,129,0.35)' }}>
                <div className="h-10 w-10 flex items-center justify-center text-lg">🗓️</div>
              </div>
            </div>
          </div>
        )}
        {isCreatorRole && (
          <div className="card">
            <div className="flex items-start justify-between">
              <div>
                <div className="text-sm text-gray-600">Room Requests</div>
                <div className="text-3xl font-bold text-gray-900 mt-1">{bookings.length}</div>
              </div>
              <div className="h-10 w-10 rounded-lg" style={{ background: 'rgba(245,158,11,0.35)' }}>
                <div className="h-10 w-10 flex items-center justify-center text-lg">🏢</div>
              </div>
            </div>
          </div>
        )}
        {isGeneralLike && (
          <div className="card">
            <div className="flex items-start justify-between">
              <div>
                <div className="text-sm text-gray-600">Past Events</div>
                <div className="text-3xl font-bold text-gray-900 mt-1">{pastRegs.length}</div>
              </div>
              <div className="h-10 w-10 rounded-lg" style={{ background: 'rgba(100,116,139,0.14)' }}>
                <div className="h-10 w-10 flex items-center justify-center text-lg">✅</div>
              </div>
            </div>
          </div>
        )}
      </div>

      <div className={mainGridClass}>
        {/* My Registered Events - only for GENERAL_USER / CLUB_ASSOCIATE */}
        {showRegistered && (
          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold">My Registered Events</h2>
              <span className="text-xs px-2 py-1 rounded-full bg-blue-50 text-blue-700 border border-blue-100">
                {registrations.length} total
              </span>
            </div>
            {loading ? (
              <div>
                <Skeleton className="w-32 mb-3" height="1.25rem" />
                <div className="space-y-3">
                  <Skeleton className="w-full" height="1.125rem" />
                  <Skeleton className="w-full" height="1.125rem" />
                </div>
              </div>
            ) : registrations.length === 0 ? (
              <div className="text-gray-500 text-sm">No registrations yet.</div>
            ) : (
              <div className="space-y-4">
                {registrations.map(ev => (
                  <div key={ev.eventId} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow" style={{ background: 'rgba(15,23,42,0.9)' }}>
                    <div className="flex items-start justify-between mb-2">
                      <h3 className="font-medium text-gray-900">{ev.title}</h3>
                      <span className="bg-blue-600/20 text-blue-300 text-xs px-2 py-1 rounded-full">Registered</span>
                    </div>
                    <div className="text-xs text-gray-500">
                      {ev.startTime && (
                        <>
                          📅 {new Date(ev.startTime).toLocaleDateString()} · {new Date(ev.startTime).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}
                        </>
                      )}
                    </div>
                    <div className="text-xs text-gray-500 mt-1">
                      📍 {ev.location || 'TBD'}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* My Created Events - for ADMIN/FACULTY/CLUB_ASSOCIATE */}
        {showCreatorPanels && (
          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold">Events I Created</h2>
              <button type="button" className="btn btn-secondary btn-sm" onClick={() => navigate('/events/create')}>Create</button>
            </div>
            {loading ? (
              <div>
                <Skeleton className="w-32 mb-3" height="1.25rem" />
                <div className="space-y-3">
                  <Skeleton className="w-full" height="1.125rem" />
                  <Skeleton className="w-full" height="1.125rem" />
                </div>
              </div>
            ) : createdEvents.length === 0 ? (
              <div className="text-gray-500 text-sm">No events created yet.</div>
            ) : (
              <div className="space-y-4">
                {createdEvents.map(ev => {
                  const start = ev.startTime ? new Date(ev.startTime) : null
                  const editable = start && (start.getTime() - now.getTime()) > 2 * 24 * 60 * 60 * 1000
                  const hasStarted = start && start.getTime() <= now.getTime()
                  const isCancelling = cancellingId === ev.id
                  const isConfirming = confirmId === ev.id
                  return (
                    <div key={ev.id} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow" style={{ background: 'rgba(15,23,42,0.9)' }}>
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <h3 className="font-medium text-gray-900">{ev.title}</h3>
                          <div className="text-xs text-gray-500">
                            {ev.startTime && (
                              <>
                                📅 {new Date(ev.startTime).toLocaleDateString()} · {new Date(ev.startTime).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}
                              </>
                            )}
                          </div>
                        </div>
                        <span className="text-xs px-2 py-1 rounded-full bg-slate-800 text-gray-700">
                          {editable ? 'Editable' : 'Locked (<2 days)'}
                        </span>
                      </div>
                    <div className="text-xs text-gray-500 mt-1">
                        📍 {ev.location || 'TBD'}
                      </div>

                      {/* Confirmation modal */}
                      {isConfirming && (
                        <div className="mt-3 p-3 rounded-lg border border-[#991B1B]/40" style={{ background: 'rgba(127,29,29,0.15)' }}>
                          <p className="text-sm text-[#FECACA] mb-3">Are you sure you want to cancel this event?</p>
                          <div className="flex gap-2">
                            <button
                              type="button"
                              className="btn btn-sm"
                              style={{ background: 'transparent', border: '1px solid #374151', color: '#D1D5DB' }}
                              onClick={() => setConfirmId(null)}
                              disabled={isCancelling}
                            >
                              Go Back
                            </button>
                            <button
                              type="button"
                              className="btn btn-sm"
                              style={{ background: '#991B1B', color: '#FECACA', borderRadius: '8px' }}
                              onClick={() => handleCancelEvent(ev.id)}
                              disabled={isCancelling}
                            >
                              {isCancelling ? (
                                <span className="flex items-center gap-2">
                                  <span className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }}></span>
                                  Cancelling...
                                </span>
                              ) : 'Confirm Cancel'}
                            </button>
                          </div>
                        </div>
                      )}

                      <div className="mt-3 flex space-x-2">
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm flex-1"
                          disabled={!editable}
                          onClick={() => editable && navigate(`/events/edit/${ev.id}`)}
                        >
                          View / Edit Event
                        </button>
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          onClick={() => navigate(`/register/${ev.id}`)}
                        >
                          Notifications
                        </button>
                        {!isConfirming && (
                          <button
                            type="button"
                            className="btn btn-sm"
                            style={{
                              background: hasStarted ? '#374151' : '#7F1D1D',
                              color: hasStarted ? '#6B7280' : '#FECACA',
                              borderRadius: '8px',
                              padding: '6px 12px',
                              cursor: hasStarted ? 'not-allowed' : 'pointer',
                              opacity: hasStarted ? 0.6 : 1,
                              transition: 'all 0.2s ease',
                            }}
                            disabled={hasStarted}
                            onClick={() => setConfirmId(ev.id)}
                            onMouseEnter={(e) => { if (!hasStarted) e.target.style.background = '#991B1B' }}
                            onMouseLeave={(e) => { if (!hasStarted) e.target.style.background = '#7F1D1D' }}
                          >
                            Cancel Event
                          </button>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        )}

        {/* My Room Bookings - only for creator roles (they can book rooms) */}
        {showCreatorPanels && (
          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold">My Room Requests</h2>
              <button type="button" className="btn btn-secondary btn-sm" onClick={() => navigate('/book-room')}>New</button>
            </div>
            {loading ? (
              <div className="text-gray-500 text-sm">Loading...</div>
            ) : bookings.length === 0 ? (
              <div className="text-gray-500 text-sm">No room requests yet.</div>
            ) : (
              <div className="space-y-4">
                {bookings.map(b => {
                  const start = b.start ? new Date(b.start) : null
                  const canCancel = b.status === 'PENDING' && start && (start.getTime() - now.getTime()) > 2 * 24 * 60 * 60 * 1000
                  const onCancel = async () => {
                    try {
                      await api.post(`/api/room-requests/${b.id}/cancel`)
                      setBookings(prev => prev.map(x => x.id === b.id ? { ...x, status: 'REJECTED' } : x))
                    } catch (err) {
                      // optional: surface error later
                    }
                  }
                  return (
                    <div key={b.id} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow" style={{ background: 'rgba(15,23,42,0.9)' }}>
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <h3 className="font-medium text-gray-900">{b.eventTitle || 'Meeting'}</h3>
                          <p className="text-sm text-gray-600">Allocated: {b.allocatedRoom || 'TBD'}</p>
                        </div>
                        <span className={`text-xs px-2 py-1 rounded-full ${getStatusBadge(b.status)}`}>
                          {(b.status || '').charAt(0) + (b.status || '').slice(1).toLowerCase()}
                        </span>
                      </div>
                      <div className="text-xs text-gray-500">
                        {b.start && (
                          <>📅 {new Date(b.start).toLocaleDateString()} · {new Date(b.start).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}</>
                        )}
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        Preferences: {b.pref1} → {b.pref2} → {b.pref3}
                      </div>
                      <div className="mt-3 flex space-x-2">
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm flex-1"
                          disabled={!canCancel}
                          onClick={canCancel ? onCancel : undefined}
                        >
                          {canCancel ? 'Cancel Request' : 'Cannot cancel (<2 days or not pending)'}
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div className="card mt-6">
        <h2 className="text-xl font-semibold mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <a href="/events" className="btn btn-primary w-full">
            Browse Events
          </a>
          <a href="/book-room" className="btn btn-secondary w-full">
            Book a Room
          </a>
          <a href="/bookings" className="btn btn-secondary w-full">
            View All Bookings
          </a>
        </div>
      </div>
    </div>
  )
}

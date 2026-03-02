import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import api from '../lib/api'

export default function Events() {
  const navigate = useNavigate()
  const [events, setEvents] = useState([])
  const [navigating, setNavigating] = useState(false)
  const [eventList, setEventList] = useState([])
  const [loading, setLoading] = useState(true)
  const [registeredEventIds, setRegisteredEventIds] = useState(() => new Set())
  const { hasRole, clubId, user } = useAuth()

  useEffect(() => {
    api.get('/api/public/events').then((res) => {
      const mapped = (res.data || []).map(e => ({ 
        id: e.id, 
        title: e.title, 
        start: e.startTime, 
        end: e.endTime,
        backgroundColor: '#3b82f6',
        borderColor: '#2563eb'
      }))
      setEvents(mapped)
      setEventList(res.data || [])
    }).catch(() => {
      setEvents([])
      setEventList([])
    }).finally(() => {
      setLoading(false)
    })
  }, [])

  useEffect(() => {
    if (!user) {
      setRegisteredEventIds(new Set())
      return
    }
    let mounted = true
    // Prefer user-linked event registrations
    api.get('/api/event-registrations/mine').then(res => {
      if (!mounted) return
      const ids = new Set((res.data || []).map(r => Number(r.eventId)).filter(n => !Number.isNaN(n)))
      setRegisteredEventIds(ids)
    }).catch(() => {
      if (!mounted) return
      setRegisteredEventIds(new Set())
    })
    return () => { mounted = false }
  }, [user])

  if (loading) {
    return (
      <div className="text-center py-12">
        <div className="mx-auto mb-6" style={{maxWidth:320}}>
          <div className="skeleton w-full h-8 mb-3 animate-pulse"></div>
          <div className="skeleton w-full h-6 mb-2 animate-pulse"></div>
          <div className="skeleton w-full h-6 animate-pulse"></div>
        </div>
        <p className="text-gray-600">Loading events...</p>
      </div>
    )
  }

  const canRegister = (evt) => {
    if (evt && registeredEventIds.has(Number(evt.id))) return false
    // creators should not register for their own events
    if (user && evt.createdBy && evt.createdBy === user.sub) return false
    if (hasRole('ADMIN') || hasRole('FACULTY')) return false
    if (hasRole('CLUB_ASSOCIATE')) {
      if (clubId && evt.clubId && evt.clubId === clubId) return false
    }
    return true
  }

  const canOpenEventPage = (evt) => {
    // The event page also hosts event notifications. Allow creators/admins to open it even if they can't register.
    if (!user) return false
    if (hasRole('ADMIN') || hasRole('FACULTY')) return true
    if (evt && evt.createdBy && evt.createdBy === user.sub) return true
    // general users / club associates can open if they are eligible to register
    return canRegister(evt)
  }

  const now = new Date()
  const in15Days = new Date(now.getTime() + 15 * 24 * 60 * 60 * 1000)
  const upcomingEvents = (eventList || [])
    .filter(event => {
      if (!event.startTime) return false
      const start = new Date(event.startTime)
      return start >= now && start <= in15Days
    })
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))

  return (
    <div>
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">EventSphere</h1>
          <p className="text-gray-600">Discover and register for upcoming events</p>
        </div>
        {(hasRole('ADMIN') || hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE')) && (
          <div className="flex gap-2">
            <Link to="/events/create" className="btn btn-primary">Create Event</Link>
          </div>
        )}
      </div>

      {/* Calendar View */}
      <div className="card mb-8 relative">
        {navigating && (
          <div className="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center rounded-lg">
            <div className="flex items-center gap-2 bg-white px-4 py-2 rounded-full shadow-md text-sm font-medium text-blue-700">
              <span className="spinner w-4 h-4 border-2 border-blue-200 border-t-blue-600"></span>
              Redirecting to registration...
            </div>
          </div>
        )}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold">Event Calendar</h2>
          <div className="text-xs text-gray-500">Click a date or an event to register (if eligible)</div>
        </div>
        <FullCalendar 
          plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
          initialView="dayGridMonth" 
          events={events} 
          height="auto"
          headerToolbar={{
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek'
          }}
          dayCellClassNames={(arg) => {
            const date = arg.date
            const dateStr = new Date(date.getTime() - (date.getTimezoneOffset() * 60000)).toISOString().split('T')[0]
            const hasEvent = events.some(e => {
              if (!e.start) return false
              const eDate = new Date(e.start)
              const eDateStr = new Date(eDate.getTime() - (eDate.getTimezoneOffset() * 60000)).toISOString().split('T')[0]
              return eDateStr === dateStr
            })
            if (hasEvent) {
              return ['cursor-pointer', 'hover:bg-blue-50/50', 'transition-colors']
            }
            return []
          }}
          dateClick={(info) => {
            const date = info.date
            const dateStr = new Date(date.getTime() - (date.getTimezoneOffset() * 60000)).toISOString().split('T')[0]
            // Find the first event on this date
            const firstEvent = eventList.find(e => {
              if (!e.startTime) return false
              const eDate = new Date(e.startTime)
              const eDateStr = new Date(eDate.getTime() - (eDate.getTimezoneOffset() * 60000)).toISOString().split('T')[0]
              return eDateStr === dateStr
            })

            if (firstEvent && canOpenEventPage(firstEvent)) {
              setNavigating(true)
              setTimeout(() => {
                navigate(`/register/${firstEvent.id}`)
              }, 400) // slight delay to show loading state
            }
          }}
          eventClick={(info) => {
            const event = eventList.find(e => e.id === info.event.id)
            if (event && canOpenEventPage(event)) {
              setNavigating(true)
              setTimeout(() => {
                navigate(`/register/${event.id}`)
              }, 400)
            }
          }}
        />
      </div>
      
      {/* Event List */}
      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold">Upcoming Events</h2>
          <span className="text-xs px-2 py-1 rounded-full bg-gray-50 text-gray-700 border border-gray-200">
            Next 15 days: {upcomingEvents.length}
          </span>
        </div>
        {upcomingEvents.length === 0 ? (
          <div className="text-center py-8">
            <div className="text-4xl mb-4">📅</div>
            <p className="text-gray-600">No upcoming events in the next 15 days</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {upcomingEvents.map(event => (
              <div key={event.id} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow" style={{ background: 'rgba(255,255,255,0.8)' }}>
                <div className="flex items-start justify-between mb-3">
                  <h3 className="text-lg font-semibold text-gray-900">{event.title}</h3>
                  <span className="bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded-full">
                    Public
                  </span>
                </div>
                
                {event.description && (
                  <p className="text-gray-600 mb-4 text-sm">{event.description}</p>
                )}
                
                <div className="space-y-2 mb-4">
                  <div className="flex items-center text-sm text-gray-500">
                    <span className="mr-2">📅</span>
                    <span>{new Date(event.startTime).toLocaleDateString()}</span>
                  </div>
                  <div className="flex items-center text-sm text-gray-500">
                    <span className="mr-2">🕐</span>
                    <span>
                      {new Date(event.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})} - 
                      {new Date(event.endTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                    </span>
                  </div>
                  <div className="flex items-center text-sm text-gray-500">
                    <span className="mr-2">📍</span>
                    <span>Location: {event.location || 'TBD'}</span>
                  </div>
                </div>
                
                {canRegister(event) ? (
                  <Link 
                    to={`/register/${event.id}`} 
                    className="btn btn-primary btn-sm w-full"
                  >
                    Register for Event
                  </Link>
                ) : (
                  registeredEventIds.has(Number(event.id)) ? (
                    <button className="btn btn-secondary btn-sm w-full" disabled>
                      Registered
                    </button>
                  ) : canOpenEventPage(event) ? (
                    <Link to={`/register/${event.id}`} className="btn btn-secondary btn-sm w-full">
                      View Event
                    </Link>
                  ) : (
                    <button className="btn btn-secondary btn-sm w-full" disabled>
                      Registration not available
                    </button>
                  )
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}



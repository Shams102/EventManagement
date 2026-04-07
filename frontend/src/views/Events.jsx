import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import api from '../lib/api'
import showToast from '../lib/toast'
import EventCalendarEvent from '../ui/EventCalendarEvent'
import EventPreviewModal from '../components/EventPreviewModal'

export default function Events() {
  const [events, setEvents] = useState([])
  const [eventList, setEventList] = useState([])
  const [loading, setLoading] = useState(true)
  const [registeredEventIds, setRegisteredEventIds] = useState(() => new Set())
  const [selectedEvent, setSelectedEvent] = useState(null)
  const { hasRole, clubId, user } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    api.get('/api/public/events').then((res) => {
      const mapped = (res.data || []).map(e => ({ 
        id: e.id, 
        title: e.title, 
        start: e.startTime, 
        end: e.endTime,
        backgroundColor: '#1f2937',
        borderColor: '#334155',
        extendedProps: {
          category: e.category || e.type || 'default',
        },
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

  const canRegister = (evt) => {
    if (evt && registeredEventIds.has(Number(evt.id))) return false
    if (user && evt.createdBy && evt.createdBy === user.sub) return false
    if (hasRole('ADMIN') || hasRole('BUILDING_ADMIN') || hasRole('CENTRAL_ADMIN')) return false
    if (hasRole('CLUB_ASSOCIATE')) {
      if (clubId && evt.clubId && evt.clubId === clubId) return false
    }
    return true
  }

  const canOpenEventPage = (evt) => {
    if (!user) return false
    if (hasRole('ADMIN') || hasRole('FACULTY')) return true
    if (evt && evt.createdBy && evt.createdBy === user.sub) return true
    return canRegister(evt)
  }

  const isAuthenticated = !!user

  const getEligibilityText = (evt) => {
    if (!evt) return ''
    if (!isAuthenticated) {
      return 'Login is required before you can register for this event.'
    }
    if (canRegister(evt)) {
      return 'You are eligible to register for this event with your current role.'
    }
    if (canOpenEventPage(evt)) {
      return 'You can view this event, but registration is not available for your role.'
    }
    return 'You are not eligible to register for this event based on your current role.'
  }

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
        {(hasRole('ADMIN') || hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('CENTRAL_ADMIN') || hasRole('BUILDING_ADMIN')) && (
          <div className="flex gap-2">
            <Link to="/events/create" className="btn btn-primary">Create Event</Link>
          </div>
        )}
      </div>

      {/* Calendar View */}
      <div className="card mb-8">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold">Event Calendar</h2>
          <div className="text-xs text-gray-500">
            Click an event to preview details and register
          </div>
        </div>
        <FullCalendar 
          plugins={[dayGridPlugin, timeGridPlugin]} 
          initialView="dayGridMonth" 
          events={events} 
          height="auto"
          headerToolbar={{
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek'
          }}
          eventContent={(arg) => {
            const category = arg.event.extendedProps.category
            return (
              <EventCalendarEvent
                title={arg.event.title}
                timeText={arg.timeText}
                category={category}
              />
            )
          }}
          eventClassNames={() => ['fc-custom-event']}
          eventDidMount={(info) => {
            if (info.el) {
              info.el.classList.add('calendar-event-wrapper')
            }
          }}
          eventClick={(info) => {
            if (info.jsEvent && typeof info.jsEvent.preventDefault === 'function') {
              info.jsEvent.preventDefault()
            }
            const clickedId = info.event.id
            const evt = eventList.find(e => String(e.id) === String(clickedId))
            if (!evt) return
            setSelectedEvent(evt)
          }}
        />
      </div>

      <EventPreviewModal
        isOpen={!!selectedEvent}
        event={selectedEvent}
        eligibilityText={getEligibilityText(selectedEvent)}
        onClose={() => setSelectedEvent(null)}
        onRegister={() => {
          if (!selectedEvent) return
          if (!user) {
            navigate('/login')
            return
          }
          if (!canOpenEventPage(selectedEvent)) {
            showToast({ message: 'You are not eligible to register for this event.', type: 'error' })
            return
          }
          navigate(`/events/${selectedEvent.id}/register`)
        }}
      />
      
      {/* Event List */}
      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold">Upcoming Events</h2>
          <span className="text-xs px-2 py-1 rounded-full bg-slate-800 text-gray-700 border border-gray-200">
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
              <div key={event.id} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow" style={{ background: 'rgba(15,23,42,0.9)' }}>
                <div className="flex items-start justify-between mb-3">
                  <h3 className="text-lg font-semibold text-gray-900">{event.title}</h3>
                  <span className="bg-blue-600/20 text-blue-300 text-xs px-2 py-1 rounded-full">
                    Public
                  </span>
                </div>
                
                {event.description && (
                  <p className="text-gray-600 mb-4 text-sm">{event.description}</p>
                )}
                
                <div className="space-y-2 mb-4">
                  <div className="flex items-center text-sm text-gray-500">
                    <span className="mr-2">📅</span>
                    <span>
                      {new Date(event.startTime).toLocaleDateString() === new Date(event.endTime).toLocaleDateString() 
                        ? new Date(event.startTime).toLocaleDateString() 
                        : `${new Date(event.startTime).toLocaleDateString()} - ${new Date(event.endTime).toLocaleDateString()}`}
                    </span>
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
                  <div className="flex items-center text-sm text-gray-500">
                    <span className="mr-2">👥</span>
                    <span>
                      {event.maxAttendees
                        ? `Capacity: ${event.currentRegistrations ?? 0} / ${event.maxAttendees}`
                        : 'Unlimited capacity'}
                    </span>
                  </div>
                </div>
                
                {canRegister(event) ? (
                  <Link 
                    to={`/events/${event.id}/register`} 
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
                    <Link to={`/events/${event.id}/register`} className="btn btn-secondary btn-sm w-full">
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



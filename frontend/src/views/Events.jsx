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
        backgroundColor: '#7c3aed',
        borderColor: '#6d28d9'
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
          <div className="skeleton w-full h-8 mb-3 animate-pulse bg-slate-800"></div>
          <div className="skeleton w-full h-6 mb-2 animate-pulse bg-slate-800"></div>
          <div className="skeleton w-full h-6 animate-pulse bg-slate-800"></div>
        </div>
        <p className="text-slate-400">Loading events...</p>
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
    if (!user) return false
    if (hasRole('ADMIN') || hasRole('FACULTY')) return true
    if (evt && evt.createdBy && evt.createdBy === user.sub) return true
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
          <h1 className="text-3xl font-bold text-slate-100">EventSphere</h1>
          <p className="text-slate-400">Discover and register for upcoming events</p>
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
          <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm z-[70] flex items-center justify-center rounded-xl">
            <div className="flex items-center gap-3 bg-slate-800 border border-slate-700 px-5 py-3 rounded-full shadow-xl text-sm font-medium text-violet-300">
              <span className="spinner w-4 h-4 border-2 border-violet-500/30 border-t-violet-500"></span>
              Redirecting to registration...
            </div>
          </div>
        )}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-slate-100">Event Calendar</h2>
          <div className="text-xs text-slate-400">Click an event to register</div>
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
          eventContent={(arg) => {
            const fullEvent = eventList.find(e => e.id === Number(arg.event.id))
            const isClickable = fullEvent && canOpenEventPage(fullEvent)
            return (
              <div className="w-full h-full relative group p-0.5" title={isClickable ? "Click to Register" : ""}>
                <div className="overflow-hidden text-ellipsis px-1 text-xs font-medium">{arg.event.title}</div>
                {isClickable && (
                  <div className="absolute opacity-0 group-hover:opacity-100 transition-opacity bg-slate-800 text-white text-xs px-2 py-1 rounded shadow-lg -top-8 left-1/2 -translate-x-1/2 whitespace-nowrap z-[60] pointer-events-none border border-slate-600">
                    Click to Register
                  </div>
                )}
              </div>
            )
          }}
          eventClick={(info) => {
            const event = eventList.find(e => e.id === info.event.id)
            if (event && canOpenEventPage(event)) {
              setNavigating(true)
              setTimeout(() => {
                navigate(`/events/register/${event.id}`)
              }, 400)
            }
          }}
        />
      </div>
      
      {/* Event List */}
      <div className="card mt-8">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-semibold text-slate-100">Upcoming Events</h2>
          <span className="text-xs px-3 py-1 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
            Next 15 days: {upcomingEvents.length}
          </span>
        </div>
        {upcomingEvents.length === 0 ? (
          <div className="text-center py-12">
            <div className="text-5xl mb-4 opacity-50">📅</div>
            <p className="text-slate-400">No upcoming events in the next 15 days</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {upcomingEvents.map(event => (
              <div key={event.id} className="border border-slate-700 rounded-xl p-5 hover:-translate-y-1 transition-all bg-[#0f172a] shadow-lg hover:shadow-[0_0_20px_rgba(139,92,246,0.1)]">
                <div className="flex items-start justify-between mb-3">
                  <h3 className="text-lg font-semibold text-slate-100 leading-tight">{event.title}</h3>
                  <span className="bg-violet-600/20 text-violet-300 border border-violet-500/20 text-xs px-2 py-1 rounded-md ml-3 shrink-0">
                    Public
                  </span>
                </div>
                
                {event.description && (
                  <p className="text-slate-400 mb-5 text-sm line-clamp-2">{event.description}</p>
                )}
                
                <div className="space-y-2.5 mb-5 bg-slate-800/50 p-3 rounded-lg border border-slate-700/50">
                  <div className="flex items-center text-sm text-slate-300">
                    <span className="mr-2.5 opacity-70">📅</span>
                    <span className="font-medium">{new Date(event.startTime).toLocaleDateString()}</span>
                  </div>
                  <div className="flex items-center text-sm text-slate-300">
                    <span className="mr-2.5 opacity-70">🕐</span>
                    <span>
                      {new Date(event.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})} - 
                      {new Date(event.endTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                    </span>
                  </div>
                  <div className="flex items-center text-sm text-slate-300">
                    <span className="mr-2.5 opacity-70">📍</span>
                    <span className="truncate" title={event.location || 'TBD'}>{event.location || 'TBD'}</span>
                  </div>
                </div>
                
                {canRegister(event) ? (
                  <Link 
                    to={`/events/register/${event.id}`}
                    className="btn btn-primary btn-sm w-full"
                  >
                    Register for Event
                  </Link>
                ) : (
                  registeredEventIds.has(Number(event.id)) ? (
                    <div className="flex items-center justify-center gap-2 bg-emerald-900/30 text-emerald-400 border border-emerald-800/50 rounded-lg py-2 text-sm font-medium w-full">
                      ✓ Already Registered
                    </div>
                  ) : canOpenEventPage(event) ? (
                    <Link to={`/events/register/${event.id}`} className="btn btn-secondary btn-sm w-full">
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

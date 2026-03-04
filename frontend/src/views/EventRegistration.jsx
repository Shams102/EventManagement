import React, { useState, useEffect, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../lib/api'
import { showToast } from '../lib/toast'
import EventNotificationsPanel from './EventNotificationsPanel'
import { useAuth } from '../lib/AuthContext'
import { motion } from 'framer-motion'

export default function EventRegistration() {
  const { eventId } = useParams()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [fullName, setFullName] = useState('')
  const [answers, setAnswers] = useState({})
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [event, setEvent] = useState(null)
  const [closed, setClosed] = useState(false)
  const [isRegistered, setIsRegistered] = useState(false)
  const { user, hasRole } = useAuth()
  const emailValid = email ? /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}$/.test(email.trim()) : true

  const isErrorMessage = (m) => {
    if (!m) return false
    return /(failed|error|closed|please enter|invalid)/i.test(String(m))
  }

  useEffect(() => {
    api.get(`/api/public/events/${eventId}`)
      .then(res => setEvent(res.data))
      .catch(() => setEvent(null))
  }, [eventId])

  useEffect(() => {
    // check if current user is registered for this event
    api.get('/api/event-registrations/mine').then(res => {
      const regs = res.data || []
      setIsRegistered(regs.some(r => Number(r.eventId) === Number(eventId)))
    }).catch(() => setIsRegistered(false))
  }, [eventId])

  const fields = useMemo(() => {
    if (!event) return []
    try {
      const arr = event.registrationSchema ? JSON.parse(event.registrationSchema) : []
      return Array.isArray(arr) ? arr : []
    } catch { return [] }
  }, [event])

  useEffect(() => {
    if (event && event.startTime) {
      const start = new Date(event.startTime)
      const deadline = new Date(start.getTime() - 2 * 24 * 60 * 60 * 1000)
      setClosed(new Date() > deadline)
    }
  }, [event])

  const isCreator = user && event && event.createdBy && user.sub === event.createdBy
  const canRegisterRole = hasRole('GENERAL_USER') || hasRole('CLUB_ASSOCIATE')
  const canViewNotifications = !!event && (isRegistered || isCreator || hasRole('ADMIN'))
  const canPostNotifications = !!event && (isCreator || hasRole('ADMIN'))

  const formattedDate = event && event.startTime ? new Date(event.startTime).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }) : ''
  const formattedTimeRange = event && event.startTime && event.endTime ? `${new Date(event.startTime).toLocaleTimeString([], {hour: 'numeric', minute: '2-digit'})} — ${new Date(event.endTime).toLocaleTimeString([], {hour: 'numeric', minute: '2-digit'})}` : ''
  const registrationDeadlineStr = event && event.startTime ? new Date(new Date(event.startTime).getTime() - 2 * 24 * 60 * 60 * 1000).toLocaleString() : ''

  const onSubmit = async (e) => {
    e.preventDefault()
    setMessage('')

    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}$/
    if (!emailPattern.test(email.trim())) {
      setMessage('Please enter a valid email address.')
      return
    }

    setLoading(true)
    
    try {
      const res = await api.post(`/api/events/${eventId}/register`, { fullName: fullName.trim() })
      const already = res?.data?.status === 'already_registered'
      setMessage(already ? 'You are already registered for this event.' : 'Registration successful!')
      showToast({ message: 'Registration successful', type: 'success' })
      setIsRegistered(true)
      setTimeout(() => {
        navigate('/events')
      }, 2000)
    } catch (err) {
      const m = 'Registration failed: ' + (err.response?.data || err.message)
      setMessage(m)
      showToast({ message: m, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  if (!event) {
    return (
      <div className="text-center py-12">
        <div className="spinner mx-auto mb-4"></div>
        <p className="text-gray-600">Loading event details...</p>
      </div>
    )
  }

  return (
    <motion.div
      className="max-w-6xl mx-auto px-2 sm:px-4"
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -8 }}
      transition={{ duration: 0.2, ease: 'easeOut' }}
    >
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-100">Event Registration</h1>
          <p className="text-slate-400">Complete your registration for this event</p>
        </div>
        <button 
          onClick={() => navigate('/events')}
          className="btn btn-secondary"
        >
          ← Back
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Event Details */}
        <div className="card">
          <h2 className="text-xl font-semibold mb-4">Event Details</h2>
          <div className="space-y-4">
            <div>
              <h3 className="font-semibold text-slate-100">{event.title}</h3>
              {event.description && (
                <p className="text-slate-400 text-sm mt-1">{event.description}</p>
              )}
            </div>
            
            <div className="space-y-2">
                <div className="flex items-center text-sm text-slate-400">
                <span className="mr-2">📅</span>
                <span>{formattedDate} • {formattedTimeRange}</span>
              </div>
              {registrationDeadlineStr && (
                <div className="flex items-center text-xs text-slate-400 mt-1">
                  Registration deadline: {registrationDeadlineStr}
                </div>
              )}
              <div className="flex items-center text-sm text-slate-400">
                <span className="mr-2">📍</span>
                <span>{event.location || 'TBD'}</span>
              </div>
              <div className="mt-3 text-sm text-slate-400">
                <div className="font-semibold text-slate-300 mb-1">Eligibility</div>
                <div className="space-y-1">
                  <div>• Eligible roles: General Users and Club Associates.</div>
                  <div>• Event creators cannot register for their own events.</div>
                  <div>• Admins and Faculty typically manage or host events instead of registering.</div>
                  <div className="text-xs text-slate-500 mt-1">Final eligibility is always enforced by the backend.</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Registration Form */}
        <div className="card">
          <h2 className="text-xl font-semibold mb-6">Registration Form</h2>

          {!canRegisterRole && (
            <div className="alert alert-error mb-4">
              Registration is only available for General Users and Club Associates.
            </div>
          )}

          {closed && (
            <div className="alert alert-error mb-4">
              Registration is closed. Registrations are allowed only until 2 days before the event start.
            </div>
          )}

          {isCreator && (
            <div className="alert alert-error mb-4">
              Event creators cannot register for their own events.
            </div>
          )}

          {isRegistered && (
            <div className="alert alert-success mb-4">
              You are already registered for this event.
            </div>
          )}
          
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="form-group">
              <label className="form-label">Full Name</label>
              <input
                type="text"
                className="form-input"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Enter your full name"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input
                type="email"
                className="form-input"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Enter your email address"
                pattern="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}$"
                title="Please enter a valid email address (example@domain.com)"
                required
                aria-invalid={!emailValid}
              />
              {email && !emailValid && <div className="text-xs text-red-600 mt-1">Please enter a valid email address</div>}
            </div>

            {/* Dynamic Answers */}
            {fields.filter(f => f !== 'full_name' && f !== 'email').map(key => (
              <div className="form-group" key={key}>
                <label className="form-label">{key.replace(/_/g,' ').replace(/\b\w/g, c => c.toUpperCase())}</label>
                <input className="form-input" value={answers[key] || ''} onChange={e=>setAnswers(prev=>({...prev,[key]:e.target.value}))} />
              </div>
            ))}

            {message && (
              <div className={`alert ${isErrorMessage(message) ? 'alert-error' : 'alert-success'}`} role={isErrorMessage(message) ? 'alert' : 'status'} aria-live={isErrorMessage(message) ? 'assertive' : 'polite'}>
                {message}
              </div>
            )}

            <button
              type="submit"
              className="btn btn-primary w-full"
              disabled={loading || closed || isCreator || !canRegisterRole || isRegistered}
            >
              {loading ? (
                <div className="flex items-center justify-center">
                  <div className="spinner mr-2"></div>
                  Registering...
                </div>
              ) : (
                isRegistered ? 'Already registered' : 'Register for Event'
              )}
            </button>
          </form>

          <div className="mt-6 rounded-xl border border-slate-800 bg-slate-900 p-4">
            <h3 className="font-semibold text-slate-100 mb-2">Registration Terms</h3>
            <div className="text-sm text-slate-400 space-y-1">
              <div>Registration is free and open to all campus members</div>
              <div>You will receive a confirmation after registration</div>
              <div>Please arrive 10 minutes before the event starts</div>
              <div>Contact the organizer if you need to cancel</div>
            </div>
          </div>
        </div>
      </div>

      {/* Event notifications (only for registered users) */}
      <EventNotificationsPanel eventId={eventId} canView={canViewNotifications} canPost={canPostNotifications} />

    </motion.div>
  )
}


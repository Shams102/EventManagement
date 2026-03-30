import React, { useState, useEffect } from 'react'
import api from '../lib/api'
import { showToast } from '../lib/toast'
import { useAuth } from '../lib/AuthContext'

export default function RoomBooking() {
  const { hasRole } = useAuth()
  const isFaculty = hasRole('FACULTY')
  
  // Original state
  const [rooms, setRooms] = useState([])
  const [roomsLoading, setRoomsLoading] = useState(false)
  const [roomsError, setRoomsError] = useState('')
  const [events, setEvents] = useState([])
  const [eventsLoading, setEventsLoading] = useState(false)
  const [eventsError, setEventsError] = useState('')
  const [eventId, setEventId] = useState('')
  const [pref1, setPref1] = useState('')
  const [pref2, setPref2] = useState('')
  const [pref3, setPref3] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [mode, setMode] = useState('event') // 'event' or 'meeting'
  const [meetingStart, setMeetingStart] = useState('')
  const [meetingEnd, setMeetingEnd] = useState('')
  const [meetingPurpose, setMeetingPurpose] = useState('')
  const [roomConflicts, setRoomConflicts] = useState([])
  const [roomWindowAvailability, setRoomWindowAvailability] = useState({})
  const [roomWindowLoading, setRoomWindowLoading] = useState(false)
  
  // Fixed time slots for meeting mode
  const timeSlots = [
    '09:00-09:50', '09:50-10:40', '10:40-11:30', '11:30-12:20',
    '12:20-13:10', '13:10-14:00', '14:00-14:50', '14:50-15:40',
    '15:40-16:30', '16:30-17:20', '17:20-18:10'
  ]
  const [useFixedSlotsForMeeting, setUseFixedSlotsForMeeting] = useState(true)
  const [meetingDate, setMeetingDate] = useState('')
  const [selectedSlots, setSelectedSlots] = useState([])
  const [fixedSlotAvailability, setFixedSlotAvailability] = useState([])
  const [fixedSlotLoading, setFixedSlotLoading] = useState(false)
  const [slotWindowAvailability, setSlotWindowAvailability] = useState({})

  // Calculate today's date in local timezone (YYYY-MM-DD format)
  const todayLocal = (() => {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  })()

  const loadBaseData = async () => {
    setRoomsLoading(true)
    setEventsLoading(true)
    setRoomsError('')
    setEventsError('')
    try {
      const [roomsRes, eventsRes] = await Promise.allSettled([
        api.get('/api/rooms'),
        api.get('/api/events/mine')
      ])

      if (roomsRes.status === 'fulfilled') {
        setRooms(roomsRes.value.data || [])
      } else {
        setRooms([])
        const data = roomsRes.reason?.response?.data
        const msg = (data && (data.error || data.message)) ? (data.error || data.message) : (roomsRes.reason?.message || 'Failed to load rooms')
        setRoomsError(String(msg))
      }

      if (eventsRes.status === 'fulfilled') {
        setEvents(eventsRes.value.data || [])
      } else {
        setEvents([])
        const data = eventsRes.reason?.response?.data
        const msg = (data && (data.error || data.message)) ? (data.error || data.message) : (eventsRes.reason?.message || 'Failed to load events')
        setEventsError(String(msg))
      }
    } finally {
      setRoomsLoading(false)
      setEventsLoading(false)
    }
  }

  useEffect(() => {
    loadBaseData()
  }, [])

  const refreshAvailabilityForEvent = (evId) => {
    const ev = events.find(e => e.id === Number(evId))
    if (!ev) return
    const start = ev.startTime
    const end = ev.endTime
    if (!start || !end) return
    loadWindowAvailability(start, end)
  }

  const toLocalDateTimeSeconds = (v) => {
    if (!v) return ''
    const s = String(v)
    if (s.length >= 19) return s.slice(0, 19)
    if (s.length === 16) return `${s}:00`
    return s
  }

  const toLocalIsoNoSeconds = (dateStr, timeStr) => {
    if (!dateStr || !timeStr) return ''
    return `${dateStr}T${timeStr}:00`
  }

  const areSlotsConsecutive = (slots) => {
    if (slots.length <= 1) return true
    const sorted = [...slots].sort((a, b) => timeSlots.indexOf(a) - timeSlots.indexOf(b))
    for (let i = 1; i < sorted.length; i++) {
      const prevIdx = timeSlots.indexOf(sorted[i - 1])
      const currIdx = timeSlots.indexOf(sorted[i])
      if (currIdx !== prevIdx + 1) return false
    }
    return true
  }

  const fetchFixedSlotAvailability = async (roomId, dateStr) => {
    if (!roomId || !dateStr) return
    try {
      setFixedSlotLoading(true)
      const res = await api.get(`/api/room-management/rooms/${roomId}/availability`, {
        params: { date: dateStr }
      })
      setFixedSlotAvailability(res.data?.availableSlots || [])
    } catch (e) {
      setFixedSlotAvailability([])
    } finally {
      setFixedSlotLoading(false)
    }
  }

  const fetchSlotWindowAvailability = async (roomId, dateStr) => {
    if (!roomId || !dateStr) return
    try {
      const checks = timeSlots.map(async (slot) => {
        const [startStr, endStr] = slot.split('-')
        const start = toLocalIsoNoSeconds(dateStr, startStr)
        const end = toLocalIsoNoSeconds(dateStr, endStr)
        const res = await api.get(`/api/rooms/${roomId}/availability`, { params: { start, end } })
        const ok = !!(res.data && (res.data.available === true || res.data.available === 'true'))
        return [slot, ok]
      })
      const settled = await Promise.allSettled(checks)
      const map = {}
      settled.forEach((r) => {
        if (r.status === 'fulfilled' && Array.isArray(r.value)) {
          map[r.value[0]] = r.value[1]
        }
      })
      setSlotWindowAvailability(map)
    } catch {
      setSlotWindowAvailability({})
    }
  }

  useEffect(() => {
    const roomId = Number(pref1)
    if (!roomId) {
      setFixedSlotAvailability([])
      setSelectedSlots([])
      setSlotWindowAvailability({})
      return
    }

    if (mode === 'meeting') {
      if (!useFixedSlotsForMeeting) return
      if (!meetingDate) {
        setFixedSlotAvailability([])
        setSelectedSlots([])
        setSlotWindowAvailability({})
        return
      }
      fetchFixedSlotAvailability(roomId, meetingDate)
      fetchSlotWindowAvailability(roomId, meetingDate)
      return
    }

    const ev = events.find(e => e.id === Number(eventId))
    const evDate = ev?.startTime ? String(ev.startTime).slice(0, 10) : ''
    if (!evDate) {
      setFixedSlotAvailability([])
      setSelectedSlots([])
      setSlotWindowAvailability({})
      return
    }
    fetchFixedSlotAvailability(roomId, evDate)
  }, [pref1, mode, eventId, meetingDate, useFixedSlotsForMeeting, events])

  useEffect(() => {
    if (mode !== 'meeting') return
    if (!useFixedSlotsForMeeting) return
    if (!meetingDate || selectedSlots.length === 0) {
      setMeetingStart('')
      setMeetingEnd('')
      return
    }

    const sorted = [...selectedSlots].sort((a, b) => timeSlots.indexOf(a) - timeSlots.indexOf(b))
    const [startStr] = sorted[0].split('-')
    const [, endStr] = sorted[sorted.length - 1].split('-')
    setMeetingStart(toLocalIsoNoSeconds(meetingDate, startStr))
    setMeetingEnd(toLocalIsoNoSeconds(meetingDate, endStr))
  }, [mode, useFixedSlotsForMeeting, meetingDate, selectedSlots])

  const refreshAvailabilityForMeeting = () => {
    if (!meetingStart || !meetingEnd) return
    const start = toLocalDateTimeSeconds(meetingStart)
    const end = toLocalDateTimeSeconds(meetingEnd)
    loadWindowAvailability(start, end)
  }

  const loadWindowAvailability = async (start, end) => {
    if (!start || !end) return
    try {
      setRoomWindowLoading(true)
      const res = await api.get('/api/rooms/availability', { params: { start, end } })
      const map = {}
      ;(res.data || []).forEach(x => {
        if (x && (x.roomId != null)) map[Number(x.roomId)] = !!x.available
      })
      setRoomWindowAvailability(map)
    } catch {
      setRoomWindowAvailability({})
    } finally {
      setRoomWindowLoading(false)
    }
  }

  useEffect(() => {
    const shouldUseWindow =
      (mode === 'event') ||
      (mode === 'meeting' && !useFixedSlotsForMeeting)

    if (!shouldUseWindow) {
      setRoomWindowAvailability({})
      return
    }

    if (mode === 'event') {
      const ev = events.find(e => e.id === Number(eventId))
      if (ev?.startTime && ev?.endTime) {
        loadWindowAvailability(ev.startTime, ev.endTime)
      }
      return
    }

    if (meetingStart && meetingEnd) {
      const start = toLocalDateTimeSeconds(meetingStart)
      const end = toLocalDateTimeSeconds(meetingEnd)
      loadWindowAvailability(start, end)
    }
  }, [mode, eventId, meetingStart, meetingEnd, events, useFixedSlotsForMeeting])

  const loadRoomConflicts = (roomId) => {
    let start, end
    if (mode === 'event') {
      const ev = events.find(e => e.id === Number(eventId))
      if (!ev || !ev.startTime || !ev.endTime) return
      start = ev.startTime
      end = ev.endTime
    } else {
      if (!meetingStart || !meetingEnd) return
      start = toLocalDateTimeSeconds(meetingStart)
      end = toLocalDateTimeSeconds(meetingEnd)
    }
    api.get(`/api/rooms/${roomId}/availability`, { params: { start, end } })
      .then(res => {
        const available = !!(res.data && (res.data.available === true || res.data.available === 'true'))
        setRoomConflicts([{
          id: `availability-${roomId}`,
          title: available ? 'This room is AVAILABLE in the selected window' : 'This room is OCCUPIED in the selected window',
          start: null,
          end: null,
          status: available ? 'AVAILABLE' : 'OCCUPIED'
        }])
      })
      .catch(() => setRoomConflicts([]))
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setMessage('')
    setLoading(true)
    
    try {
      if (isFaculty) {
        if (!pref1) {
          setMessage('Please select a room')
          showToast({ message: 'Please select a room', type: 'error' })
          return
        }
        let payload
        if (mode === 'event') {
          if (!eventId) { setMessage('Please select an event'); return }
          payload = { eventId: Number(eventId), roomId: Number(pref1) }
        } else {
          if (!meetingStart || !meetingEnd || !meetingPurpose) { setMessage('Please provide meeting start, end, and purpose'); return }
          payload = {
            roomId: Number(pref1),
            start: toLocalDateTimeSeconds(meetingStart),
            end: toLocalDateTimeSeconds(meetingEnd),
            purpose: meetingPurpose
          }
        }
        const res = await api.post('/api/faculty/bookings', payload)
        if (res.status === 200) {
          setMessage('Booked and approved immediately for the selected room.')
          showToast({ message: 'Room booked — approved', type: 'success' })
          setEventId(''); setPref1(''); setPref2(''); setPref3('')
          setMeetingStart(''); setMeetingEnd(''); setMeetingPurpose('')
          setRoomConflicts([])
        } else {
          setMessage('Booking failed')
          showToast({ message: 'Booking failed', type: 'error' })
        }
      } else {
        if (mode === 'event') {
          if (!pref1 || !pref2 || !pref3) {
            setMessage('Please select three room preferences')
            return
          }
          if (new Set([pref1, pref2, pref3]).size < 3) {
            setMessage('Please select three different rooms')
            return
          }
          if (!eventId) { setMessage('Please select an event'); return }

          const payload = {
            pref1RoomId: Number(pref1),
            pref2RoomId: Number(pref2),
            pref3RoomId: Number(pref3),
            eventId: Number(eventId)
          }
          const res = await api.post('/api/room-requests', payload)
          if (res.status === 200) {
            setMessage('Request submitted. Admin approval pending. Official confirmation 2 days before the event/meeting.')
            setEventId(''); setPref1(''); setPref2(''); setPref3('')
            setMeetingStart(''); setMeetingEnd(''); setMeetingPurpose('')
            setRoomConflicts([])
          } else {
            setMessage('Request failed')
          }
        } else {
          if (!pref1) {
            setMessage('Please select a room')
            showToast({ message: 'Please select a room', type: 'error' })
            return
          }
          if (!meetingStart || !meetingEnd || !meetingPurpose) { setMessage('Please provide meeting start, end, and purpose'); return }

          const payload = {
            roomId: Number(pref1),
            purpose: meetingPurpose,
            meetingStart: toLocalDateTimeSeconds(meetingStart),
            meetingEnd: toLocalDateTimeSeconds(meetingEnd)
          }
          const res = await api.post('/api/room-requests/meeting', payload)
          if (res.status === 200) {
            setMessage('Request submitted. Admin approval pending. Official confirmation 2 days before the event/meeting.')
            setEventId(''); setPref1(''); setPref2(''); setPref3('')
            setMeetingStart(''); setMeetingEnd(''); setMeetingPurpose('')
            setRoomConflicts([])
          } else {
            setMessage('Request failed')
          }
        }
      }
    } catch (err) {
      const data = err.response?.data
      const detail = (data && (data.error || data.message)) ? (data.error || data.message)
        : (typeof data === 'string' ? data : err.message)
      setMessage('Booking failed: ' + detail)
    } finally {
      if (mode === 'meeting' && useFixedSlotsForMeeting && Number(pref1) > 0 && meetingDate) {
        fetchFixedSlotAvailability(Number(pref1), meetingDate)
      }
      setLoading(false)
    }
  }

  const selectedRoomInfo = rooms.find(room => room.id === Number(pref1))

  const isErrorMessage = (m) => {
    if (!m) return false
    return /(failed|error|not available|please select|invalid)/i.test(String(m))
  }

  return (
    <div className="max-w-6xl mx-auto px-2 sm:px-4">
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-[#E5E7EB]">Book a Room</h1>
          <p className="text-sm text-[#9CA3AF]">Reserve conference rooms, lecture halls, and meeting spaces</p>
        </div>
        <div className="text-xs text-[#9CA3AF]">
          {isFaculty ? 'Faculty: instant approval' : 'Club: admin approval'}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
        {/* Booking Form */}
        <div>
          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-[#E5E7EB]">Room Reservation</h2>
              <div className="inline-flex rounded-xl p-1" style={{ background: '#0F172A', border: '1px solid #1F2937' }}>
                <button
                  type="button"
                  className="btn btn-sm"
                  style={{
                    background: mode === 'event' ? '#3B82F6' : 'transparent',
                    color: mode === 'event' ? '#fff' : '#9CA3AF',
                    transition: 'all 0.2s ease',
                    borderRadius: '8px',
                    border: 'none'
                  }}
                  onClick={() => setMode('event')}
                >
                  For Event
                </button>
                <button
                  type="button"
                  className="btn btn-sm"
                  style={{
                    background: mode === 'meeting' ? '#3B82F6' : 'transparent',
                    color: mode === 'meeting' ? '#fff' : '#9CA3AF',
                    transition: 'all 0.2s ease',
                    borderRadius: '8px',
                    border: 'none'
                  }}
                  onClick={() => setMode('meeting')}
                >
                  For Meeting
                </button>
              </div>
            </div>
            
            <form onSubmit={onSubmit} className="space-y-6">
              {(roomsError || eventsError) && (
                <div className="alert alert-error">
                  <div className="space-y-1">
                    {roomsError && <div><strong>Rooms:</strong> {roomsError}</div>}
                    {eventsError && <div><strong>Events:</strong> {eventsError}</div>}
                    <button
                      type="button"
                      className="btn btn-sm btn-secondary mt-2"
                      onClick={() => loadBaseData()}
                      disabled={roomsLoading || eventsLoading}
                    >
                      {roomsLoading || eventsLoading ? 'Reloading…' : 'Reload data'}
                    </button>
                  </div>
                </div>
              )}
              {mode === 'event' && (!eventId || !Number(pref1)) && (
                <div className="text-xs text-gray-500">
                  Select an event and a room to see fixed-slot availability.
                </div>
              )}

              {mode === 'event' ? (
                <div className="form-group">
                  <label className="form-label">Select Event</label>
                  <select
                    className="form-select"
                    value={eventId}
                    onChange={(e)=>{
                      const v = e.target.value
                      setEventId(v)
                      setMessage('')
                      if (v) refreshAvailabilityForEvent(v)
                    }}
                    required
                  >
                    <option value="">Choose one of your events...</option>
                    {events.map(ev => (
                      <option key={ev.id} value={ev.id}>{ev.title}</option>
                    ))}
                  </select>
                </div>
              ) : (
                <>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="form-group">
                        <label className="form-label">Meeting Date</label>
                        <input
                          type="date"
                          className="form-input"
                          value={meetingDate}
                          min={todayLocal}
                          onChange={e => { 
                            const val = e.target.value
                            if (val < todayLocal) return
                            setMeetingDate(val); setMessage(''); setSelectedSlots([]) 
                          }}
                        />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Selected Slots</label>
                        <input
                          type="text"
                          className="form-input"
                          value={selectedSlots.length ? selectedSlots.join(', ') : ''}
                          readOnly
                          placeholder="Select up to 3 consecutive slots"
                        />
                      </div>
                    </div>
                    {Number(pref1) > 0 && meetingDate && (
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <div className="text-sm text-[#9CA3AF]">Select up to 3 consecutive slots</div>
                        <div className="text-sm text-[#9CA3AF]">
                          {fixedSlotLoading ? 'Checking availability…' : ''}
                        </div>
                      </div>
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                        {timeSlots.map(slot => {
                          const fixedOk = fixedSlotAvailability.includes(slot)
                          const windowOk = slotWindowAvailability[slot]
                          const hasWindow = Object.keys(slotWindowAvailability).length > 0
                          let isAvailable = fixedOk && (hasWindow ? windowOk !== false : true)
                          // disable slot if start time already passed for selected date
                          if (meetingDate === todayLocal) {
                            const [startStr] = slot.split('-')
                            const slotStart = new Date(`${meetingDate}T${startStr}:00`)
                            if (slotStart.getTime() <= Date.now()) {
                              isAvailable = false
                            }
                          }
                          const isSelected = selectedSlots.includes(slot)
                          const slotCls = isSelected
                            ? 'bg-blue-600 text-white border-blue-700'
                            : isAvailable
                              ? 'bg-emerald-900/30 text-emerald-300 border-emerald-700/40 hover:bg-emerald-900/50'
                              : 'bg-rose-900/30 text-rose-400 border-rose-700/40'
                          return (
                            <button
                              key={slot}
                              type="button"
                              disabled={!isAvailable}
                              onClick={() => {
                                if (!isAvailable) return
                                setMessage('')
                                setSelectedSlots(prev => {
                                  const next = prev.includes(slot) ? prev.filter(s => s !== slot) : [...prev, slot]
                                  if (next.length > 3) return prev
                                  if (!areSlotsConsecutive(next)) return prev
                                  return next
                                })
                              }}
                              className={`p-2 rounded-lg text-sm border transition ${slotCls} ${isAvailable ? 'cursor-pointer' : 'cursor-not-allowed opacity-60'}`}
                            >
                              <div className="font-medium">{slot}</div>
                            </button>
                          )
                        })}
                      </div>
                      <div className="flex flex-wrap gap-2 text-xs text-[#9CA3AF]">
                        <span className="inline-flex items-center gap-2 rounded-full border border-emerald-700/40 bg-emerald-900/30 text-emerald-300 px-3 py-1">
                          Available
                        </span>
                        <span className="inline-flex items-center gap-2 rounded-full border border-rose-700/40 bg-rose-900/30 text-rose-400 px-3 py-1">
                          Occupied
                        </span>
                        <span className="inline-flex items-center gap-2 rounded-full border border-blue-700 bg-blue-600 text-white px-3 py-1">
                          Selected
                        </span>
                      </div>
                    </div>
                  )}

                  <div className="form-group">
                    <label className="form-label">Meeting Purpose</label>
                    <textarea
                      className="form-input"
                      rows={2}
                      value={meetingPurpose}
                      onChange={e => setMeetingPurpose(e.target.value)}
                      placeholder="e.g., Project review, committee meeting"
                    />
                  </div>
                </>
              )}

              <div className={`grid grid-cols-1 ${isFaculty || mode === 'meeting' ? 'md:grid-cols-1' : 'md:grid-cols-3'} gap-4`}>
                <div className="form-group">
                  <label className="form-label">{isFaculty ? 'Select Room' : 'Preference 1'}</label>
                  <select
                    className="form-select"
                    value={pref1}
                    onChange={(e)=>{
                      const v = e.target.value
                      setPref1(v)
                      setRoomConflicts([])
                      if (v) loadRoomConflicts(Number(v))
                    }}
                    required
                  >
                    <option value="">Choose a room...</option>
                    {rooms.map(room => {
                      const ok = roomWindowAvailability[Number(room.id)]
                      const hasWindow = Object.keys(roomWindowAvailability).length > 0
                      const disabled = hasWindow ? !ok : false
                      return (
                        <option key={room.id} value={room.id} disabled={disabled}>
                          {room.name}{room.location ? ` - ${room.location}` : ''} (Capacity: {room.capacity})
                          {hasWindow ? (ok ? ' — AVAILABLE' : ' — OCCUPIED') : ''}
                        </option>
                      )
                    })}
                  </select>
                  {rooms.length === 0 && !roomsLoading && !roomsError && (
                    <div className="text-xs text-gray-500 mt-1">
                      No rooms loaded. Use “Reload data” above or check backend is running.
                    </div>
                  )}
                  {roomWindowLoading && (
                    <div className="text-xs text-gray-500 mt-1">Checking availability…</div>
                  )}
                </div>

                {!isFaculty && mode === 'event' && (
                  <>
                    <div className="form-group">
                      <label className="form-label">Preference 2</label>
                      <select
                        className="form-select"
                        value={pref2}
                        onChange={(e)=>setPref2(e.target.value)}
                        required
                      >
                        <option value="">Choose a room...</option>
                        {rooms.map(room => {
                          const ok = roomWindowAvailability[Number(room.id)]
                          const hasWindow = Object.keys(roomWindowAvailability).length > 0
                          const disabled = hasWindow ? !ok : false
                          return (
                            <option key={room.id} value={room.id} disabled={disabled}>
                              {room.name}{room.location ? ` - ${room.location}` : ''} (Capacity: {room.capacity})
                              {hasWindow ? (ok ? ' — AVAILABLE' : ' — OCCUPIED') : ''}
                            </option>
                          )
                        })}
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label">Preference 3</label>
                      <select
                        className="form-select"
                        value={pref3}
                        onChange={(e)=>setPref3(e.target.value)}
                        required
                      >
                        <option value="">Choose a room...</option>
                        {rooms.map(room => {
                          const ok = roomWindowAvailability[Number(room.id)]
                          const hasWindow = Object.keys(roomWindowAvailability).length > 0
                          const disabled = hasWindow ? !ok : false
                          return (
                            <option key={room.id} value={room.id} disabled={disabled}>
                              {room.name}{room.location ? ` - ${room.location}` : ''} (Capacity: {room.capacity})
                              {hasWindow ? (ok ? ' — AVAILABLE' : ' — OCCUPIED') : ''}
                            </option>
                          )
                        })}
                      </select>
                    </div>
                  </>
                )}
              </div>

              {mode === 'event' && Number(pref1) > 0 && eventId && (
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="text-sm text-[#9CA3AF]">Fixed-slot availability (from weekly class timetable)</div>
                    <div className="text-sm text-[#9CA3AF]">
                      {fixedSlotLoading ? 'Checking availability…' : ''}
                    </div>
                  </div>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                    {timeSlots.map(slot => {
                      const isAvailable = fixedSlotAvailability.includes(slot)
                      return (
                        <div
                          key={slot}
                          className={`p-2 rounded-lg text-sm border ${isAvailable ? 'bg-emerald-900/30 border-emerald-700/40 text-emerald-300' : 'bg-rose-900/30 border-rose-700/40 text-rose-400'}`}
                        >
                          <div className="font-medium">{slot}</div>
                          <div className="text-xs">{isAvailable ? 'Available' : 'Occupied'}</div>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )}

              {/* Message Display */}
              {message && (
                <div className={`alert ${isErrorMessage(message) ? 'alert-error' : 'alert-success'}`} role={isErrorMessage(message) ? 'alert' : 'status'} aria-live={isErrorMessage(message) ? 'assertive' : 'polite'}>
                  {message}
                </div>
              )}

              {/* Submit Button */}
              <div className="flex justify-end">
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={loading}
                >
                  {loading ? (
                    <div className="flex items-center justify-center">
                      <div className="spinner mr-2"></div>
                      Submitting Request...
                    </div>
                  ) : (
                    'Submit Request'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>

        {/* Sidebar */}
        <div>
          <div className="card">
            <h3 className="text-lg font-semibold text-[#E5E7EB] mb-4">Selected Room</h3>
            {!Number(pref1) ? (
              <div className="text-center py-6">
                <div className="text-3xl mb-3">🏢</div>
                <p className="text-sm text-[#9CA3AF]">Select a room preference to see details.</p>
              </div>
            ) : !selectedRoomInfo ? (
              <div className="text-sm text-[#9CA3AF]">Room details unavailable.</div>
            ) : (
              <div className="space-y-4">
                <div>
                  <div className="font-semibold text-[#E5E7EB] text-lg">{selectedRoomInfo.name}</div>
                  <div className="text-sm text-[#9CA3AF]">{selectedRoomInfo.location || '—'}</div>
                </div>
                <div className="p-4 rounded-xl" style={{ background: '#0F172A', border: '1px solid #1F2937' }}>
                  <div className="text-sm text-[#9CA3AF] mb-1">Room Capacity</div>
                  <div className="text-2xl font-bold text-[#60A5FA] flex items-center gap-2">
                    <span>👥</span>
                    <span>{selectedRoomInfo.capacity} seats</span>
                  </div>
                </div>
                {Object.keys(roomWindowAvailability).length > 0 && (
                  <div className="text-sm">
                    Availability in selected window:{' '}
                    <span className={roomWindowAvailability[Number(selectedRoomInfo.id)] ? 'text-emerald-400 font-semibold' : 'text-rose-400 font-semibold'}>
                      {roomWindowAvailability[Number(selectedRoomInfo.id)] ? '✓ AVAILABLE' : '✗ OCCUPIED'}
                    </span>
                  </div>
                )}
              </div>
            )}
          </div>

          {roomConflicts.length > 0 && (
            <div className="card mt-6">
              <h3 className="text-lg font-semibold text-[#E5E7EB] mb-4">Room Status</h3>
              <div className="space-y-2 text-sm text-[#9CA3AF]">
                {roomConflicts.map(rc => (
                  <div key={rc.id} className="flex items-center justify-between">
                    <div>
                      <div className="font-medium text-[#E5E7EB]">{rc.title}</div>
                      <div className="text-xs text-[#9CA3AF]">
                        {rc.start && new Date(rc.start).toLocaleString()} – {rc.end && new Date(rc.end).toLocaleString()}
                      </div>
                    </div>
                    <span className={`text-xs px-2 py-1 rounded-full ${rc.status === 'AVAILABLE' ? 'bg-emerald-900/40 text-emerald-300' : 'bg-rose-900/40 text-rose-400'}`}>
                      {rc.status}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Booking Guidelines */}
          <div className="card mt-6">
            <h3 className="text-lg font-semibold text-[#E5E7EB] mb-4">Booking Guidelines</h3>
            <div className="text-sm text-[#9CA3AF] space-y-2">
              {mode === 'event' ? (
                <>
                  <div className="flex items-start">
                    <span className="mr-2">⏰</span>
                    <span><strong>Event Booking:</strong> Minimum 5 days advance notice required</span>
                  </div>
                  <div className="flex items-start">
                    <span className="mr-2">📅</span>
                    <span>Official confirmation will be sent 2 days before the event</span>
                  </div>
                  <div className="flex items-start">
                    <span className="mr-2">⚠️</span>
                    <span>Admin approval required for all event room bookings</span>
                  </div>
                </>
              ) : (
                <>
                  <div className="flex items-start">
                    <span className="mr-2">🚀</span>
                    <span><strong>Meeting Booking:</strong> Can be booked for same day</span>
                  </div>
                  <div className="flex items-start">
                    <span className="mr-2">✅</span>
                    <span>Faculty: Immediate approval without admin processing</span>
                  </div>
                  <div className="flex items-start">
                    <span className="mr-2">⏳</span>
                    <span>Club Associates: Admin approval required</span>
                  </div>
                </>
              )}
              <div className="flex items-start">
                <span className="mr-2">👥</span>
                <span>{isFaculty ? 'Faculty: Select 1 room (immediate approval)' : 'Club Associates: Select 3 room preferences (admin approval)'}</span>
              </div>
              <div className="flex items-start">
                <span className="mr-2">🏛️</span>
                <span>Only Faculty, Club Associates, and Admin can book rooms</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

import React, { useEffect, useMemo, useState } from 'react'
import { showToast } from '../lib/toast'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import api from '../lib/api'
import Card from '../ui/Card'
import Button from '../ui/Button'
import TimeSelect from '../ui/TimeSelect'

const FIELD_OPTIONS = [
  { key: 'full_name', label: 'Full Name' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Phone Number' },
  { key: 'student_id', label: 'Student ID' },
  { key: 'department', label: 'Department' },
  { key: 'year', label: 'Year' },
  { key: 'tshirt_size', label: 'T-shirt Size' },
  { key: 'dietary_pref', label: 'Dietary Preference' },
  { key: 'comments', label: 'Comments' }
]

export default function CreateEvent() {
  const { hasRole, clubId } = useAuth()
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [startDate, setStartDate] = useState('')
  const [startTime, setStartTime] = useState('')
  const [endDate, setEndDate] = useState('')
  const [endTime, setEndTime] = useState('')
  const [loc, setLoc] = useState('')
  const [club, setClub] = useState(clubId || '')
  const [selected, setSelected] = useState(['full_name', 'email'])
  const [maxAttendees, setMaxAttendees] = useState('')
  const [buildingId, setBuildingId] = useState('')
  const [buildings, setBuildings] = useState([])
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [timingModel, setTimingModel] = useState('SINGLE_DAY')
  const [timeSlots, setTimeSlots] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.get('/api/public/buildings')
      .then(res => setBuildings((res.data || []).filter(b => b.id && b.name)))
      .catch(() => setBuildings([]))
  }, [])

  const getMinDate = () => {
    const now = new Date()
    const pad = (n) => String(n).padStart(2, '0')
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
  }

  const allowed = hasRole('ADMIN') || hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('CENTRAL_ADMIN') || hasRole('BUILDING_ADMIN')
  const isFormValid = title.trim() && startDate && startTime && endDate && endTime && buildingId

  const toggleField = (key) => {
    setSelected((prev) => prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key])
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!allowed) { setError('Not authorized') ; return }
    if (!buildingId) { setError('Please select a building'); return }
    setError('')
    setMessage('')
    try {
      setLoading(true)
      const registrationSchema = JSON.stringify(selected)

      // client-side validation: no past dates and end after start
      const now = new Date()
      if (!startDate || !startTime || !endDate || !endTime) { setError('Please provide both start and end'); setLoading(false); return }
      const startLocal = `${startDate}T${startTime}`
      const endLocal = `${endDate}T${endTime}`
      const startDt = new Date(`${startLocal}:00`)
      const endDt = new Date(`${endLocal}:00`)
      if (startDt < now) { setError('Start time cannot be in the past'); setLoading(false); return }
      if (endDt <= startDt) { setError('End time must be after start time'); setLoading(false); return }
      
      if (timingModel === 'FLEXIBLE') {
        if (timeSlots.length === 0) {
          setError('Flexible timing requires at least one custom time slot'); setLoading(false); return;
        }
        for (let i = 0; i < timeSlots.length; i++) {
          const ts = timeSlots[i];
          if (!ts.date || !ts.start || !ts.end) {
            setError('Please complete all date and time fields for flexible slots'); setLoading(false); return;
          }
          const s = new Date(`${ts.date}T${ts.start}:00`);
          const e = new Date(`${ts.date}T${ts.end}:00`);
          if (e <= s) {
            setError(`Slot ${i + 1} end time must be after its start time`); setLoading(false); return;
          }
        }
      }

      // Send local time as 'YYYY-MM-DDTHH:MM:SS' so backend LocalDateTime stores the same local time.
      const startValue = `${startLocal}:00`
      const endValue = `${endLocal}:00`
      const res = await api.post('/api/events', {
        title: title.trim(),
        description: description.trim(),
        start: startValue,
        end: endValue,
        buildingId: Number(buildingId),
        location: loc.trim() || undefined,
        clubId: club || undefined,
        maxAttendees: maxAttendees ? Number(maxAttendees) : undefined,
        registrationSchema,
        timingModel: timingModel,
        timeSlots: timingModel === 'FLEXIBLE' ? timeSlots.map(ts => ({
          slotStart: `${ts.date}T${ts.start}:00`,
          slotEnd: `${ts.date}T${ts.end}:00`
        })) : undefined
      })
      if (res.status === 200) {
        setMessage('Event created')
        showToast({ message: 'Event created', type: 'success' })
        setTimeout(() => navigate('/events'), 600)
      } else {
        setError('Failed to create event')
        showToast({ message: 'Failed to create event', type: 'error' })
      }
    } catch (err) {
      const errData = err.response?.data
      const errMsg = typeof errData === 'object' ? (errData.details || errData.error || 'Failed to create event') : (errData || 'Failed to create event')
      setError(errMsg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-[900px] mx-auto">
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-[#E5E7EB]">Create Event</h1>
          <p className="text-sm text-[#9CA3AF]">Provide event details and choose registration fields for attendees.</p>
        </div>
        <Button type="button" variant="secondary" onClick={() => navigate('/events')}>Back</Button>
      </div>

      {!allowed ? (
        <div className="alert alert-error">You are not authorized to create events.</div>
      ) : (
        <Card className="p-8">
          <form onSubmit={onSubmit} className="space-y-8">
            <Card className="p-6">
              <div className="text-sm font-semibold text-[#E5E7EB]">Event Details</div>
              <div className="mt-1 text-sm text-[#9CA3AF]">Title, description, building, location and schedule.</div>
              <div className="mt-6 border-t border-[#1F2937]" />

              <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="form-group">
                  <label className="form-label">Event Title</label>
                  <input className="form-input" value={title} onChange={(e)=>setTitle(e.target.value)} placeholder="e.g. Hackathon Kickoff" required />
                </div>
                <div className="form-group">
                  <label className="form-label">
                    Building <span className="text-red-400">*</span>
                  </label>
                  <select
                    className="form-input"
                    value={buildingId}
                    onChange={(e) => setBuildingId(e.target.value)}
                    required
                  >
                    <option value="">Select a building...</option>
                    {buildings.map(b => (
                      <option key={b.id} value={b.id}>{b.name}{b.code ? ` (${b.code})` : ''}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="mt-5 grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="form-group">
                  <label className="form-label">Location (optional)</label>
                  <input className="form-input" value={loc} onChange={(e)=>setLoc(e.target.value)} placeholder="e.g. Auditorium A" />
                </div>
                <div className="form-group">
                  <label className="form-label">Club ID (optional)</label>
                  <input className="form-input" value={club} onChange={(e)=>setClub(e.target.value)} placeholder="Leave blank to use your club automatically" />
                </div>
              </div>

              <div className="mt-5 form-group">
                <label className="form-label">Description</label>
                <textarea className="form-input" rows={4} value={description} onChange={(e)=>setDescription(e.target.value)} placeholder="Optional details attendees should know" />
              </div>

              <div className="mt-5 grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="form-group">
                  <label className="form-label">Start</label>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <input
                      type="date"
                      className="form-input"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      min={getMinDate()}
                      required
                    />
                    <TimeSelect value={startTime} onChange={setStartTime} required />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">End</label>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <input
                      type="date"
                      className="form-input"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      min={startDate || getMinDate()}
                      required
                    />
                    <TimeSelect value={endTime} onChange={setEndTime} required />
                  </div>
                  {startDate && endDate && startDate !== endDate && (
                    <div className="mt-4 p-4 rounded-lg bg-[#0F172A] border border-[#1F2937] sm:col-span-2">
                      <label className="form-label text-purple-400">✨ Multi-Day Event Settings</label>
                      <select 
                        className="form-input mt-2" 
                        value={timingModel}
                        onChange={(e) => setTimingModel(e.target.value)}
                      >
                        <option value="SINGLE_DAY">Treat as single continuous block (Legacy - binds 24/7)</option>
                        <option value="MULTI_DAY_FIXED">Fixed Daily Schedule (Repeats start/end time each day)</option>
                        <option value="MULTI_DAY_CONTINUOUS">Continuous Overnight Event (e.g. Hackathons)</option>
                        <option value="FLEXIBLE">Flexible (Custom times for each day)</option>
                      </select>
                      
                      {timingModel === 'SINGLE_DAY' && (
                        <div className="mt-2 text-xs text-rose-400">Warning: This locks the room 24/7 across all selected days, causing massive scheduling conflicts. Consider using 'Fixed Daily Schedule'.</div>
                      )}
                      
                      {timingModel === 'MULTI_DAY_FIXED' && (
                        <div className="mt-2 text-xs text-emerald-400">The room will exclusively be reserved from {startTime || 'Start'} to {endTime || 'End'} on every calendar day within the range.</div>
                      )}
                      
                      {timingModel === 'MULTI_DAY_CONTINUOUS' && (
                        <div className="mt-2 text-xs text-amber-400">Event runs continuously across midnight without stopping. Note: The building must allow overnight hours.</div>
                      )}
                      
                      {timingModel === 'FLEXIBLE' && (
                        <div className="mt-4 space-y-3 border-t border-[#1F2937] pt-4">
                          <label className="text-sm font-semibold text-[#E5E7EB]">Custom Time Slots Builder</label>
                          {timeSlots.map((ts, idx) => (
                             <div key={idx} className="flex flex-col sm:flex-row gap-2 items-center bg-[#111827] p-2 rounded border border-[#374151]">
                               <input type="date" className="form-input text-sm py-1" value={ts.date || ''} onChange={(e) => {
                                 const newSlots = [...timeSlots];
                                 newSlots[idx].date = e.target.value;
                                 setTimeSlots(newSlots);
                               }} min={startDate} max={endDate} />
                               <input type="time" className="form-input text-sm py-1" value={ts.start || ''} onChange={(e) => {
                                 const newSlots = [...timeSlots];
                                 newSlots[idx].start = e.target.value;
                                 setTimeSlots(newSlots);
                               }} />
                               <span className="text-[#9CA3AF] hidden sm:block">to</span>
                               <input type="time" className="form-input text-sm py-1" value={ts.end || ''} onChange={(e) => {
                                 const newSlots = [...timeSlots];
                                 newSlots[idx].end = e.target.value;
                                 setTimeSlots(newSlots);
                               }} />
                               <button type="button" className="p-1 px-2 text-rose-400 hover:text-rose-300 font-bold bg-[#1F2937] rounded" onClick={() => {
                                 setTimeSlots(timeSlots.filter((_, i) => i !== idx));
                               }}>×</button>
                             </div>
                          ))}
                          <button type="button" className="btn btn-secondary btn-sm mt-1" onClick={() => setTimeSlots([...timeSlots, { date: '', start: '', end: '' }])}>
                            + Add Custom Slot
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>

              <div className="mt-5 form-group">
                <label className="form-label">Maximum Attendees</label>
                <input
                  type="number"
                  className="form-input"
                  value={maxAttendees}
                  onChange={(e) => setMaxAttendees(e.target.value)}
                  min="1"
                  placeholder="Leave blank for unlimited"
                />
              </div>
            </Card>

            <Card className="p-6">
              <div className="text-sm font-semibold text-[#E5E7EB]">Registration Fields</div>
              <div className="mt-1 text-sm text-[#9CA3AF]">Choose what attendees must fill during registration.</div>
              <div className="mt-6 border-t border-[#1F2937]" />

              <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                {FIELD_OPTIONS.map(opt => {
                  const checked = selected.includes(opt.key)
                  return (
                    <label
                      key={opt.key}
                      className={[
                        'group cursor-pointer select-none rounded-xl border px-3 py-2.5 transition-all duration-200',
                        'bg-[#0F172A] border-[#1F2937] hover:border-[#3B82F6]/50 hover:bg-white/5',
                        checked ? 'ring-1 ring-[#3B82F6]/35 border-[#3B82F6]/60' : ''
                      ].join(' ')}
                    >
                      <span className="flex items-center gap-3">
                        <span
                          className={[
                            'flex h-4 w-4 items-center justify-center rounded-[6px] border transition-all duration-200',
                            checked ? 'bg-[#3B82F6] border-[#3B82F6]' : 'bg-transparent border-[#374151] group-hover:border-[#3B82F6]/60'
                          ].join(' ')}
                          aria-hidden="true"
                        >
                          <svg
                            viewBox="0 0 20 20"
                            className={checked ? 'h-3 w-3 text-white' : 'h-3 w-3 text-transparent'}
                            fill="currentColor"
                          >
                            <path
                              fillRule="evenodd"
                              d="M16.704 5.29a1 1 0 0 1 .006 1.414l-7.6 7.66a1 1 0 0 1-1.42-.002L3.29 9.954a1 1 0 1 1 1.42-1.408l3.28 3.307 6.89-6.94a1 1 0 0 1 1.414-.006Z"
                              clipRule="evenodd"
                            />
                          </svg>
                        </span>
                        <span className="text-sm text-[#E5E7EB]">{opt.label}</span>
                      </span>
                      <input
                        type="checkbox"
                        className="sr-only"
                        checked={checked}
                        onChange={() => toggleField(opt.key)}
                      />
                    </label>
                  )
                })}
              </div>

              <p className="text-xs text-[#9CA3AF] mt-4">Full Name and Email are preselected by default.</p>
            </Card>

            {error && <div className="alert alert-error" role="alert" aria-live="assertive">{error}</div>}
            {message && <div className="alert alert-success" role="status" aria-live="polite">{message}</div>}

            <div className="flex flex-col sm:flex-row gap-3 sm:justify-end pt-2">
              <Button type="button" variant="secondary" onClick={() => navigate('/events')} disabled={loading}>Cancel</Button>
              <Button type="submit" disabled={loading || !isFormValid}>
                {loading ? 'Creating...' : 'Create Event'}
              </Button>
            </div>
          </form>
        </Card>
      )}
    </div>
  )
}

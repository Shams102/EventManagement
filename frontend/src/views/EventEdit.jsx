import React, { useEffect, useMemo, useState } from 'react'
import { showToast } from '../lib/toast'
import { useNavigate, useParams } from 'react-router-dom'
import api from '../lib/api'
import TimeSelect from '../ui/TimeSelect'
import Card from '../ui/Card'
import Button from '../ui/Button'

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

export default function EventEdit() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [startDate, setStartDate] = useState('')
  const [startTime, setStartTime] = useState('')
  const [endDate, setEndDate] = useState('')
  const [endTime, setEndTime] = useState('')
  const [loc, setLoc] = useState('')
  const [club, setClub] = useState('')
  const [selected, setSelected] = useState(['full_name', 'email'])
  const [maxAttendees, setMaxAttendees] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const getMinDate = useMemo(() => {
    const now = new Date()
    const pad = (n) => String(n).padStart(2,'0')
    return `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}`
  }, [])

  useEffect(() => {
    api.get(`/api/public/events/${id}`)
      .then(res => {
        const ev = res.data
        setTitle(ev.title || '')
        setDescription(ev.description || '')
        setLoc(ev.location || '')
        setClub(ev.clubId || '')
        setMaxAttendees(ev.maxAttendees != null ? String(ev.maxAttendees) : '')
        if (ev.startTime) {
          const s = String(ev.startTime).slice(0, 16) // YYYY-MM-DDTHH:MM
          const [d, t] = s.split('T')
          setStartDate(d || '')
          setStartTime(t || '')
        }
        if (ev.endTime) {
          const s = String(ev.endTime).slice(0, 16)
          const [d, t] = s.split('T')
          setEndDate(d || '')
          setEndTime(t || '')
        }
        try {
          const schema = ev.registrationSchema ? JSON.parse(ev.registrationSchema) : []
          if (Array.isArray(schema) && schema.length > 0) {
            setSelected(schema)
          }
        } catch {
          // ignore parse errors
        }
      })
      .catch(() => setError('Failed to load event'))
      .finally(() => setLoading(false))
  }, [id])

  const toggleField = (key) => {
    setSelected(prev => prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key])
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setMessage('')
    setSaving(true)
    try {
      if (!startDate || !startTime || !endDate || !endTime) {
        setError('Please provide start and end date/time values')
        return
      }
      // client-side validation: no past dates and end after start
      const now = new Date()
      const startLocal = `${startDate}T${startTime}`
      const endLocal = `${endDate}T${endTime}`
      const startDt = new Date(`${startLocal}:00`)
      const endDt = new Date(`${endLocal}:00`)
      if (startDt < now) { setError('Start time cannot be in the past'); return }
      if (endDt <= startDt) { setError('End time must be after start time'); return }

      // datetime-local presents local time as 'YYYY-MM-DDTHH:MM'. Append seconds and send as-is
      const startValue = `${startLocal}:00`
      const endValue = `${endLocal}:00`
      const registrationSchema = JSON.stringify(selected)
      const res = await api.put(`/api/events/${id}`, {
        title: title.trim(),
        description: description.trim(),
        start: startValue,
        end: endValue,
        location: loc.trim() || undefined,
        clubId: club || undefined,
        maxAttendees: maxAttendees ? Number(maxAttendees) : undefined,
        registrationSchema
      })
      if (res.status === 200) {
        setMessage('Event updated')
        showToast({ message: 'Event updated', type: 'success' })
        setTimeout(() => navigate('/dashboard'), 800)
      } else {
        setError('Failed to update event')
        showToast({ message: 'Failed to update event', type: 'error' })
      }
    } catch (err) {
      setError(err.response?.data || 'Failed to update event')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="text-center py-12">
        <div className="spinner mx-auto mb-4"></div>
        <p className="text-[#9CA3AF]">Loading event...</p>
      </div>
    )
  }

  return (
    <div className="max-w-[900px] mx-auto">
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-[#E5E7EB]">Edit Event</h1>
          <p className="text-sm text-[#9CA3AF]">Update event details. Editing is blocked within 2 days of the start time.</p>
        </div>
        <Button type="button" variant="secondary" onClick={() => navigate('/dashboard')}>Back</Button>
      </div>

      <Card className="p-8">
        <form onSubmit={onSubmit} className="space-y-8">
          <Card className="p-6">
            <div className="text-sm font-semibold text-[#E5E7EB]">Event Details</div>
            <div className="mt-1 text-sm text-[#9CA3AF]">Update title, location and schedule.</div>
            <div className="mt-6 border-t border-[#1F2937]" />

            <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-5">
              <div className="form-group">
                <label className="form-label">Event Title</label>
                <input className="form-input" value={title} onChange={e => setTitle(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Location (optional)</label>
                <input className="form-input" value={loc} onChange={e => setLoc(e.target.value)} />
              </div>
            </div>

            <div className="mt-5 form-group">
              <label className="form-label">Description</label>
              <textarea className="form-input" rows={4} value={description} onChange={e => setDescription(e.target.value)} />
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
                    min={getMinDate}
                    required
                    style={{ colorScheme: 'dark' }}
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
                    min={startDate || getMinDate}
                    required
                    style={{ colorScheme: 'dark' }}
                  />
                  <TimeSelect value={endTime} onChange={setEndTime} required />
                </div>
              </div>
            </div>

            <div className="mt-5 form-group">
              <label className="form-label">Club ID (optional)</label>
              <input className="form-input" value={club} onChange={e => setClub(e.target.value)} />
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
            <Button type="button" variant="secondary" onClick={() => navigate('/dashboard')} disabled={saving}>Cancel</Button>
            <Button type="submit" disabled={saving}>
              {saving ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}

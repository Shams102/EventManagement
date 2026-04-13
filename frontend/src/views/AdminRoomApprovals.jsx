import React, { useEffect, useState, useCallback } from 'react'
import api from '../lib/api'
import { useAuth } from '../lib/AuthContext'

/**
 * Room booking approvals for legacy ADMIN (full access) and scoped BUILDING_ADMIN.
 * CENTRAL_ADMIN uses role/club admin only — no room queue here.
 *
 * Supports per-slot (per-day) room allocation for multi-day events.
 */
const LARGE_HALL_TYPES = new Set(['AUDITORIUM', 'SEMINAR_HALL', 'LECTURE_HALL'])
const NORMAL_ROOM_TYPES = new Set(['LAB', 'MEETING_ROOM', 'CLASSROOM'])

function roomMatchesApprovalScope(roomType, approvalScope) {
  if (!approvalScope || !roomType) return true
  const t = String(roomType)
  if (approvalScope === 'LARGE_HALL') return LARGE_HALL_TYPES.has(t)
  if (approvalScope === 'NORMAL_ROOM') return NORMAL_ROOM_TYPES.has(t)
  return true
}

function formatDateOnly(value) {
  if (!value) return 'N/A'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleDateString()
}

function formatTimeOnly(value) {
  if (!value) return 'N/A'
  const d = new Date(value)
  return Number.isNaN(d.getTime())
    ? String(value)
    : d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function formatDateTime(value) {
  if (!value) return 'N/A'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleString()
}

export default function AdminRoomApprovals() {
  const { hasRole } = useAuth()
  const [requests, setRequests] = useState([])
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')

  const load = async ({ silent } = {}) => {
    if (silent) {
      setRefreshing(true)
    } else {
      setLoading(true)
    }
    setError('')
    try {
      const [reqRes, roomsRes] = await Promise.all([
        api.get('/api/admin/room-requests?status=PENDING'),
        api.get('/api/rooms')
      ])
      setRequests(Array.isArray(reqRes.data) ? reqRes.data : [])
      setRooms(Array.isArray(roomsRes.data) ? roomsRes.data : [])
    } catch (e) {
      setError('Failed to load data')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    load({ silent: false })
  }, [])

  const approveSlotBased = async (id, slotAllocations) => {
    setError('')
    const req = requests.find(r => r.id === id)
    const group = req?.splitGroupId
    try {
      await api.post(`/api/admin/room-requests/${id}/approve`, { slotAllocations })
      setRequests(prev => prev.filter(r => {
        if (r.id === id) return false
        if (group && r.splitGroupId === group) return false
        return true
      }))
    } catch (e) {
      const msg = e?.response?.data || e?.message || 'Unknown error'
      setError('Approve failed: ' + (typeof msg === 'string' ? msg : JSON.stringify(msg)))
    }
  }

  const approveLegacy = async (id, allocatedRoomId) => {
    if (!allocatedRoomId) { setError('Select a room to allocate'); return }
    setError('')
    const req = requests.find(r => r.id === id)
    const group = req?.splitGroupId
    try {
      await api.post(`/api/admin/room-requests/${id}/approve`, { allocatedRoomId })
      setRequests(prev => prev.filter(r => {
        if (r.id === id) return false
        if (group && r.splitGroupId === group) return false
        return true
      }))
    } catch (e) {
      const msg = e?.response?.data || e?.message || 'Unknown error'
      setError('Approve failed: ' + (typeof msg === 'string' ? msg : JSON.stringify(msg)))
    }
  }

  const reject = async (id) => {
    setError('')
    try {
      await api.post(`/api/admin/room-requests/${id}/reject`)
      setRequests(prev => prev.filter(r => r.id !== id))
    } catch (e) {
      const msg = e?.response?.data || e?.message || 'Unknown error'
      setError('Reject failed: ' + (typeof msg === 'string' ? msg : JSON.stringify(msg)))
    }
  }

  if (!hasRole('ADMIN') && !hasRole('BUILDING_ADMIN')) return null

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col">
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between shrink-0">
        <div className="space-y-2 min-w-0 pr-2">
          <h1 className="text-2xl sm:text-3xl font-bold text-[#E5E7EB] tracking-tight">
            Room Booking Approvals
          </h1>
          <p className="text-sm sm:text-base text-[#9CA3AF] max-w-2xl leading-relaxed">
            Approve or reject pending room booking requests. For multi-day events, you can assign different rooms per day.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-secondary inline-flex items-center justify-center gap-2 min-w-[7.5rem] shrink-0 self-start sm:self-auto"
          onClick={() => load({ silent: true })}
          disabled={refreshing || loading}
        >
          {refreshing ? (
            <>
              <span className="spinner shrink-0" aria-hidden />
              <span>Refreshing…</span>
            </>
          ) : (
            <>
              <svg className="w-4 h-4 text-[#9CA3AF] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden>
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              <span>Refresh</span>
            </>
          )}
        </button>
      </div>

      {error && (
        <div className="alert alert-error mb-6 shrink-0" role="alert">
          {error}
        </div>
      )}

      {loading ? (
        <div
          className="flex min-h-[60vh] w-full flex-col items-center justify-center px-4 py-8"
          aria-busy="true"
          aria-label="Loading approvals"
        >
          <div className="card w-full max-w-md text-center py-12 px-8 rounded-2xl border border-[#1F2937] bg-[#111827] shadow-xl shadow-black/25">
            <div className="spinner mx-auto mb-5" />
            <p className="text-[#E5E7EB] font-medium">Loading pending requests…</p>
            <p className="text-sm text-[#9CA3AF] mt-2">Please wait</p>
          </div>
        </div>
      ) : requests.length === 0 ? (
        <div className="flex min-h-[60vh] w-full flex-col items-center justify-center px-4 py-8">
          <div
            className="w-full max-w-lg rounded-2xl border border-[#1F2937] bg-[#111827] px-8 py-10 sm:py-12 text-center shadow-xl shadow-black/30"
            role="status"
            aria-live="polite"
          >
            <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-400 ring-1 ring-emerald-500/30">
              <svg className="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden>
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <h2 className="text-xl sm:text-2xl font-bold text-[#E5E7EB] tracking-tight">
              No Pending Requests
            </h2>
            <p className="mt-4 text-sm sm:text-base leading-relaxed text-[#9CA3AF] max-w-sm mx-auto">
              You&apos;re all caught up. New booking requests will appear here.
            </p>
            <button
              type="button"
              className="btn btn-secondary mt-8 inline-flex items-center justify-center gap-2"
              onClick={() => load({ silent: true })}
              disabled={refreshing}
            >
              {refreshing ? (
                <>
                  <span className="spinner shrink-0" aria-hidden />
                  Checking…
                </>
              ) : (
                <>
                  <svg className="w-4 h-4 text-[#9CA3AF] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden>
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  Refresh
                </>
              )}
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-4 pb-10">
          {requests.map(req => (
            <ApprovalItem
              key={req.id}
              req={req}
              rooms={rooms}
              onApproveSlots={approveSlotBased}
              onApproveLegacy={approveLegacy}
              onReject={reject}
              hasRole={hasRole}
            />
          ))}
        </div>
      )}
    </div>
  )
}

/* ─────────────────────────────── Approval Card ───────────────────────────── */

function ApprovalItem({ req, rooms, onApproveSlots, onApproveLegacy, onReject, hasRole }) {
  const hasMultiSlots = req.slots && req.slots.length > 1
  const isSuperRoomAdmin = hasRole('ADMIN')

  // Per-slot allocations: { slotId: roomId }
  const [slotAllocs, setSlotAllocs] = useState({})
  // Single-room fallback for single-day events
  const [singleAlloc, setSingleAlloc] = useState('')
  // Per-slot availability data: { slotId: [{ roomId, name, available, conflicts, ... }] }
  const [slotAvailability, setSlotAvailability] = useState({})
  const [loadingAvailability, setLoadingAvailability] = useState(false)
  const [conflicts, setConflicts] = useState(null)
  const [loadingConflicts, setLoadingConflicts] = useState(false)
  const [approving, setApproving] = useState(false)

  // Filter rooms by building + approval scope (for the single-room fallback)
  const allocatableRooms = rooms.filter((r) => {
    if (req.buildingId != null && r.buildingId != null && Number(r.buildingId) !== Number(req.buildingId)) {
      return false
    }
    if (!isSuperRoomAdmin && hasRole('BUILDING_ADMIN') && req.approvalScope) {
      return roomMatchesApprovalScope(r.type, req.approvalScope)
    }
    return true
  })

  // Load per-slot availability from the new endpoint
  const loadSlotAvailability = useCallback(async () => {
    if (!hasMultiSlots) return
    setLoadingAvailability(true)
    try {
      const results = {}
      // Fetch all slot availability in parallel
      const fetches = req.slots.map(async (slot) => {
        try {
          const res = await api.get(`/api/rooms/slot-availability?slotId=${slot.id}`)
          results[slot.id] = Array.isArray(res.data) ? res.data : []
        } catch {
          results[slot.id] = []
        }
      })
      await Promise.all(fetches)
      setSlotAvailability(results)
    } finally {
      setLoadingAvailability(false)
    }
  }, [hasMultiSlots, req.slots])

  // Auto-load per-slot availability when the card mounts (only for multi-day)
  useEffect(() => {
    if (hasMultiSlots) {
      loadSlotAvailability()
    }
  }, [hasMultiSlots, loadSlotAvailability])

  const checkConflicts = async () => {
    setLoadingConflicts(true)
    try {
      const res = await api.get(`/api/admin/room-requests/${req.id}/conflicts`)
      setConflicts(res.data)
    } catch {
      // ignore
    } finally {
      setLoadingConflicts(false)
    }
  }

  // Get filtered available rooms for a specific slot
  const getSlotRooms = (slotId) => {
    const avail = slotAvailability[slotId]
    if (!avail || avail.length === 0) return allocatableRooms.map(r => ({ ...r, roomId: r.id, available: true, conflicts: [] }))
    // Filter by building + scope
    return avail.filter(r => {
      if (req.buildingId != null && r.buildingId != null && Number(r.buildingId) !== Number(req.buildingId)) {
        return false
      }
      if (!isSuperRoomAdmin && hasRole('BUILDING_ADMIN') && req.approvalScope) {
        return roomMatchesApprovalScope(r.type, req.approvalScope)
      }
      return true
    })
  }

  const handleSlotChange = (slotId, roomId) => {
    setSlotAllocs(prev => ({ ...prev, [slotId]: roomId }))
  }

  // "Apply to all" — fill all slots with the same room
  const applyToAll = (roomId) => {
    if (!roomId || !req.slots) return
    const newAllocs = {}
    req.slots.forEach(s => { newAllocs[s.id] = Number(roomId) })
    setSlotAllocs(newAllocs)
  }

  const handleApprove = async () => {
    setApproving(true)
    try {
      if (hasMultiSlots) {
        // Check all slots are allocated
        const allFilled = req.slots.every(s => slotAllocs[s.id])
        if (!allFilled) {
          // Try to fill missing from first selected
          const firstSelected = Object.values(slotAllocs).find(v => v)
          if (firstSelected) {
            const filled = { ...slotAllocs }
            req.slots.forEach(s => {
              if (!filled[s.id]) filled[s.id] = firstSelected
            })
            await onApproveSlots(req.id, filled)
          } else {
            // Fall back — nothing selected at all
            await onApproveLegacy(req.id, null) // will show "Select a room" error
          }
          return
        }
        await onApproveSlots(req.id, slotAllocs)
      } else {
        // Single-day — use legacy path
        await onApproveLegacy(req.id, Number(singleAlloc))
      }
    } finally {
      setApproving(false)
    }
  }

  return (
    <div className="card">
      <div className="flex flex-col gap-4">
        {/* ─── Request Info ─── */}
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="flex-1 min-w-0">
            <div className="text-lg font-semibold text-[#E5E7EB]">{req.eventTitle}</div>
            {req.buildingName && (
              <div className="mt-1 text-sm text-[#A78BFA]">
                Building: <span className="font-medium text-[#C4B5FD]">{req.buildingName}</span>
              </div>
            )}
            {req.approvalScope && (
              <div className="text-sm text-[#9CA3AF]">
                Your lane: <span className="font-semibold text-[#34D399]">{req.approvalScope.replace('_', ' ')}</span>
                {req.pref1RoomType && (
                  <span className="text-[#6B7280]"> (prefs are {req.pref1RoomType} tier)</span>
                )}
              </div>
            )}
            {req.splitPart && req.splitGroupId && (
              <div className="text-xs text-amber-400/90 mt-1">
                Split approval: approving this row rejects other pending parts in the same group ({String(req.splitGroupId).slice(0, 8)}…).
              </div>
            )}
            <div className="mt-1 text-sm text-[#9CA3AF]">Starts: {formatDateTime(req.start)}</div>
            <div className="text-sm text-[#9CA3AF]">Requested by: {req.requestedBy}</div>
            <div className="text-sm text-[#9CA3AF] mt-2">
              Preferences:
              <span className="font-semibold text-[#60A5FA] ml-1">{req.pref1}</span> →
              <span className="font-semibold text-[#60A5FA] ml-1">{req.pref2}</span> →
              <span className="font-semibold text-[#60A5FA] ml-1">{req.pref3}</span>
            </div>
            {req.registrationCount !== undefined && (
              <div className="text-sm font-medium mt-1 text-[#A78BFA]">
                Registrations: {req.registrationCount}
              </div>
            )}

            {/* Conflict check (preference-level) */}
            <div className="mt-3">
              {!conflicts ? (
                <button
                  type="button"
                  className="text-sm text-[#60A5FA] hover:text-[#93C5FD] hover:underline"
                  onClick={checkConflicts}
                  disabled={loadingConflicts}
                >
                  {loadingConflicts ? 'Checking conflicts...' : 'Check timetable conflicts'}
                </button>
              ) : (
                <div className="text-sm p-3 rounded-lg border border-[#1F2937] bg-[#0F172A]">
                  <div className="font-semibold mb-1 text-[#E5E7EB]">Conflicts Check:</div>
                  {Object.keys(conflicts).length === 0 ? (
                    <span className="text-emerald-400">No conflicts found.</span>
                  ) : (
                    <ul className="list-disc pl-5 space-y-1 text-rose-400">
                      {Object.entries(conflicts).map(([pref, issues]) => (
                        <li key={pref}>
                          <strong className="text-[#E5E7EB]">
                            Room {String(req.pref1Id) === String(pref) ? req.pref1
                              : String(req.pref2Id) === String(pref) ? req.pref2
                              : String(req.pref3Id) === String(pref) ? req.pref3
                              : pref}:
                          </strong>{' '}
                          {issues.length === 0 ? <span className="text-emerald-400">Clear</span> : issues.join(', ')}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* ─── Single-day allocation (legacy) ─── */}
          {!hasMultiSlots && (
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 shrink-0">
              <select
                className="form-select min-w-[12rem]"
                value={singleAlloc}
                onChange={(e) => setSingleAlloc(e.target.value)}
              >
                <option value="" disabled>Allocate room...</option>
                {allocatableRooms.map(r => (
                  <option key={r.id} value={r.id}>{r.buildingName ? `${r.buildingName} - ` : ''}{r.name} ({r.capacity || 0})</option>
                ))}
              </select>
              <button type="button" className="btn btn-primary btn-sm" disabled={approving} onClick={handleApprove}>
                {approving ? 'Approving…' : 'Approve'}
              </button>
              <button type="button" className="btn btn-secondary btn-sm" onClick={() => onReject(req.id)}>Reject</button>
            </div>
          )}
        </div>

        {/* ─── Multi-day per-slot allocation ─── */}
        {hasMultiSlots && (
          <div className="mt-2 p-4 bg-[#0F172A] border border-[#1F2937] rounded-xl">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold px-2 py-1 bg-purple-500/20 text-purple-300 rounded border border-purple-500/30">
                  {req.timingModel?.replace(/_/g, ' ') || 'MULTI DAY'} — {req.slots.length} Days
                </span>
                {loadingAvailability && <span className="spinner" style={{ width: 16, height: 16 }} aria-hidden />}
              </div>
              {/* Apply-to-all shortcut */}
              {req.slots.length > 1 && slotAllocs[req.slots[0]?.id] && (
                <button
                  type="button"
                  className="text-xs text-[#60A5FA] hover:text-[#93C5FD] hover:underline"
                  onClick={() => applyToAll(slotAllocs[req.slots[0].id])}
                >
                  Apply Day 1 room to all ↓
                </button>
              )}
            </div>

            <div className="space-y-3">
              {req.slots.map((slot, idx) => {
                const slotRooms = getSlotRooms(slot.id)
                const selectedRoom = slotAllocs[slot.id] || ''
                return (
                  <div key={slot.id} className="flex flex-col sm:flex-row sm:items-center gap-2 p-3 rounded-lg bg-[#111827] border border-[#1F2937]">
                    <div className="min-w-[10rem] shrink-0">
                      <div className="text-sm font-semibold text-[#E5E7EB]">
                        Day {idx + 1}
                      </div>
                      <div className="text-xs text-[#9CA3AF]">
                        {formatDateOnly(slot.slotStart)}
                      </div>
                      <div className="text-xs text-[#6B7280]">
                        {formatTimeOnly(slot.slotStart)} – {formatTimeOnly(slot.slotEnd)}
                      </div>
                    </div>
                    <select
                      className="form-select flex-1 min-w-[14rem]"
                      value={selectedRoom}
                      onChange={(e) => handleSlotChange(slot.id, Number(e.target.value))}
                    >
                      <option value="" disabled>Select room for Day {idx + 1}…</option>
                      {slotRooms.map(r => {
                        const isAvailable = r.available !== false
                        const conflictHint = !isAvailable && r.conflicts?.length
                          ? ` (${r.conflicts[0].length > 40 ? r.conflicts[0].slice(0, 40) + '…' : r.conflicts[0]})`
                          : !isAvailable ? ' (Unavailable)' : ''
                        return (
                          <option
                            key={r.roomId || r.id}
                            value={r.roomId || r.id}
                            disabled={!isAvailable}
                          >
                            {r.buildingName ? `${r.buildingName} - ` : ''}{r.name} ({r.capacity || 0}){conflictHint}
                          </option>
                        )
                      })}
                    </select>
                    {selectedRoom && (
                      <span className="text-xs text-emerald-400 shrink-0">✓</span>
                    )}
                  </div>
                )
              })}
            </div>

            {/* Action buttons */}
            <div className="flex items-center gap-2 mt-4 pt-3 border-t border-[#1F2937]">
              <button
                type="button"
                className="btn btn-primary btn-sm"
                disabled={approving}
                onClick={handleApprove}
              >
                {approving ? 'Approving…' : `Approve (${req.slots.length} slots)`}
              </button>
              <button type="button" className="btn btn-secondary btn-sm" onClick={() => onReject(req.id)}>
                Reject
              </button>
              {Object.keys(slotAllocs).length > 0 && Object.keys(slotAllocs).length < req.slots.length && (
                <span className="text-xs text-amber-400 ml-2">
                  {req.slots.length - Object.keys(slotAllocs).length} slot(s) unassigned — will use first selected room
                </span>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

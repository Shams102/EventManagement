import React, { useEffect, useState } from 'react'
import api from '../lib/api'
import { useAuth } from '../lib/AuthContext'

export default function AdminRoleRequests() {
  const { hasRole } = useAuth()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await api.get('/api/admin/role-requests')
      setRows(Array.isArray(res.data) ? res.data : [])
    } catch (e) {
      setError('Failed to load requests')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const act = async (id, action) => {
    setError('')
    setMsg('')
    try {
      await api.post(`/api/admin/role-requests/${id}/${action}`)
      setMsg(action === 'approve' ? 'Approved' : 'Rejected')
      setRows(rows.filter(r => r.id !== id))
    } catch (e) {
      setError('Action failed')
    }
  }

  if (!hasRole('ADMIN') && !hasRole('CENTRAL_ADMIN')) return null

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col">
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-[#E5E7EB] tracking-tight">Role Approval</h1>
          <p className="text-sm sm:text-base text-[#9CA3AF]">Review and approve or reject user role upgrade requests.</p>
        </div>
        <button type="button" className="btn btn-secondary" onClick={load} disabled={loading}>Refresh</button>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}
      {msg && <div className="alert alert-success mb-4">{msg}</div>}

      {loading ? (
        <div className="card text-center py-12">
          <div className="spinner mx-auto mb-4"></div>
          <div className="text-[#9CA3AF]">Loading requests...</div>
        </div>
      ) : rows.length === 0 ? (
        <div className="card text-center py-12">
          <div className="text-4xl mb-4">🧾</div>
          <div className="text-lg font-semibold text-[#E5E7EB]">No pending requests</div>
          <div className="text-[#9CA3AF] mt-1">New role upgrade requests will appear here.</div>
        </div>
      ) : (
        <div className="card p-0 overflow-hidden">
          <div className="px-6 py-4 border-b border-[#1F2937] bg-gradient-to-b from-[#111827] to-[#0F172A]">
            <div className="text-sm font-semibold text-[#E5E7EB]">Pending requests</div>
            <div className="text-sm text-[#9CA3AF]">{rows.length} waiting for review</div>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-[#0F172A] text-left">
                <tr>
                  <th className="px-6 py-3 text-xs font-semibold text-[#9CA3AF]">Username</th>
                  <th className="px-6 py-3 text-xs font-semibold text-[#9CA3AF]">Email</th>
                  <th className="px-6 py-3 text-xs font-semibold text-[#9CA3AF]">Requested Role</th>
                  <th className="px-6 py-3 text-xs font-semibold text-[#9CA3AF]">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1F2937]">
                {rows.map(r => (
                  <tr key={r.id} className="hover:bg-[#0F172A]">
                    <td className="px-6 py-4 text-sm font-medium text-[#E5E7EB]">{r.username}</td>
                    <td className="px-6 py-4 text-sm text-[#9CA3AF]">{r.email}</td>
                    <td className="px-6 py-4">
                      <span className="inline-flex items-center rounded-full border border-[#1F2937] bg-[#111827] px-3 py-1 text-xs font-semibold text-[#C4B5FD]">
                        {r.requestedRole}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <button className="btn btn-primary btn-sm" onClick={() => act(r.id, 'approve')}>Approve</button>
                        <button className="btn btn-secondary btn-sm" onClick={() => act(r.id, 'reject')}>Reject</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

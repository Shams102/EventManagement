import React, { useState, useEffect } from 'react'
import api from '../lib/api'
import { useAuth } from '../lib/AuthContext'
import Card from '../ui/Card'
import Button from '../ui/Button'
import { showToast } from '../lib/toast'

export default function Profile() {
  const { user } = useAuth()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  
  const [editMode, setEditMode] = useState(false)
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [fullName, setFullName] = useState('')
  
  const [passMode, setPassMode] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const loadProfile = async () => {
    setLoading(true)
    try {
      const res = await api.get('/api/profile')
      const data = res.data
      setProfile(data)
      setEmail(data.email || '')
      setPhone(data.phoneNumber || '')
      setFullName(data.fullName || '')
    } catch (e) {
      setError('Failed to load profile details')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadProfile()
  }, [])

  const handleUpdateProfile = async (e) => {
    e.preventDefault()
    try {
      await api.put('/api/profile', { email, phoneNumber: phone, fullName })
      showToast({ message: 'Profile updated successfully', type: 'success' })
      setEditMode(false)
      loadProfile()
    } catch (e) {
      showToast({ message: 'Failed to update profile', type: 'error' })
    }
  }

  const handleUpdatePassword = async (e) => {
    e.preventDefault()
    if (newPassword !== confirmPassword) {
      showToast({ message: 'New passwords do not match', type: 'error' })
      return
    }
    try {
      await api.put('/api/profile/password', { currentPassword, newPassword })
      showToast({ message: 'Password updated successfully', type: 'success' })
      setPassMode(false)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (e) {
      showToast({ message: e.response?.data || 'Failed to update password', type: 'error' })
    }
  }

  if (loading) return <div className="text-center py-12"><div className="spinner mx-auto"></div></div>
  if (error) return <div className="alert alert-error">{error}</div>
  if (!profile) return null

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-[#E5E7EB]">Your Profile</h1>
        <p className="text-sm text-[#9CA3AF]">Manage your personal information and security settings.</p>
      </div>

      <div className="space-y-6">
        <Card className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-[#E5E7EB]">Personal Information</h2>
            {!editMode && (
              <Button type="button" variant="secondary" onClick={() => setEditMode(true)}>Edit</Button>
            )}
          </div>
          
          {editMode ? (
            <form onSubmit={handleUpdateProfile} className="space-y-4 text-sm mt-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="form-group">
                  <label className="form-label">Full Name</label>
                  <input type="text" className="form-input" value={fullName} onChange={e => setFullName(e.target.value)} />
                </div>
                <div className="form-group">
                  <label className="form-label">Email</label>
                  <input type="email" className="form-input" value={email} onChange={e => setEmail(e.target.value)} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Phone Number</label>
                  <input type="text" className="form-input" value={phone} onChange={e => setPhone(e.target.value)} />
                </div>
              </div>
              <div className="flex justify-end gap-2 mt-4">
                <Button type="button" variant="secondary" onClick={() => {
                  setEditMode(false)
                  setEmail(profile.email || '')
                  setPhone(profile.phoneNumber || '')
                  setFullName(profile.fullName || '')
                }}>Cancel</Button>
                <Button type="submit">Save Changes</Button>
              </div>
            </form>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm mt-4">
              <div>
                <div className="text-[#9CA3AF] mb-1">Username (Read-only)</div>
                <div className="text-[#E5E7EB] font-medium">{profile.username}</div>
              </div>
              <div>
                <div className="text-[#9CA3AF] mb-1">Full Name</div>
                <div className="text-[#E5E7EB] font-medium">{profile.fullName || '—'}</div>
              </div>
              <div>
                <div className="text-[#9CA3AF] mb-1">Email</div>
                <div className="text-[#E5E7EB] font-medium">{profile.email}</div>
              </div>
              <div>
                <div className="text-[#9CA3AF] mb-1">Phone Number</div>
                <div className="text-[#E5E7EB] font-medium">{profile.phoneNumber || '—'}</div>
              </div>
              <div>
                <div className="text-[#9CA3AF] mb-1">Roles</div>
                <div className="flex gap-2 flex-wrap">
                  {profile.roles.map(r => (
                    <span key={r} className="bg-[#1E293B] border border-[#334155] text-xs px-2 py-1 rounded">
                      {r}
                    </span>
                  ))}
                </div>
              </div>
              {profile.clubId && (
                <div>
                  <div className="text-[#9CA3AF] mb-1">Club ID</div>
                  <div className="text-[#E5E7EB] font-medium">{profile.clubId}</div>
                </div>
              )}
            </div>
          )}
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-[#E5E7EB]">Security</h2>
            {!passMode && (
              <Button type="button" variant="secondary" onClick={() => setPassMode(true)}>Change Password</Button>
            )}
          </div>
          
          {passMode ? (
            <form onSubmit={handleUpdatePassword} className="space-y-4 max-w-sm mt-4">
              <div className="form-group">
                <label className="form-label">Current Password</label>
                <input type="password" className="form-input" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required minLength={6} />
              </div>
              <div className="form-group">
                <label className="form-label">New Password</label>
                <input type="password" className="form-input" value={newPassword} onChange={e => setNewPassword(e.target.value)} required minLength={8} />
              </div>
              <div className="form-group">
                <label className="form-label">Confirm New Password</label>
                <input type="password" className="form-input" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} required minLength={8} />
              </div>
              <div className="flex justify-end gap-2 mt-4">
                <Button type="button" variant="secondary" onClick={() => {
                  setPassMode(false)
                  setCurrentPassword('')
                  setNewPassword('')
                  setConfirmPassword('')
                }}>Cancel</Button>
                <Button type="submit">Update Password</Button>
              </div>
            </form>
          ) : (
            <div className="text-sm text-[#9CA3AF] mt-4">
              Ensure your account is using a long, random password to stay secure.
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}

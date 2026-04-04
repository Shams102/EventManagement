import React, { useState } from 'react'
import { Link, useSearchParams, useNavigate } from 'react-router-dom'
import api from '../lib/api'
import Card from '../ui/Card'
import Button from '../ui/Button'
import { showToast } from '../lib/toast'

export default function ResetPassword() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const navigate = useNavigate()

  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (!token) {
    return (
      <div className="flex min-h-[80vh] items-center justify-center">
        <Card className="w-full max-w-md p-8 text-center">
          <div className="text-rose-500 mb-4 text-4xl">⚠️</div>
          <h2 className="text-xl font-bold text-[#E5E7EB] mb-2">Invalid Reset Link</h2>
          <p className="text-[#9CA3AF] mb-6">The password reset link is invalid or missing the security token.</p>
          <Link to="/forgot-password">
            <Button className="w-full">Request New Link</Button>
          </Link>
        </Card>
      </div>
    )
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    if (password !== confirmPassword) {
      setError('Passwords do not match')
      return
    }
    setLoading(true)
    setError('')
    try {
      await api.post('/api/auth/reset-password', { token, newPassword: password })
      showToast({ message: 'Password reset successfully. You can now login.', type: 'success' })
      navigate('/login')
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to reset password. The link might be expired.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-[80vh] items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <Card className="w-full max-w-md p-8">
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold tracking-tight text-[#E5E7EB]">Choose New Password</h2>
          <p className="mt-2 text-sm text-[#9CA3AF]">
            Please enter your new strong password below.
          </p>
        </div>

        <form className="space-y-6" onSubmit={onSubmit}>
          {error && <div className="alert alert-error">{error}</div>}
          
          <div className="form-group">
            <label className="form-label">New Password</label>
            <input
              type="password"
              required
              minLength={8}
              className="form-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Confirm New Password</label>
            <input
              type="password"
              required
              minLength={8}
              className="form-input"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          <div>
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? 'Reseting...' : 'Reset Password'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}

import React, { useState } from 'react'
import api from '../lib/api'
import { useAuth } from '../lib/AuthContext'
import { useNavigate, Link } from 'react-router-dom'
import { jwtDecode } from 'jwt-decode'
import { showToast } from '../lib/toast'
import Card from '../ui/Card'
import Button from '../ui/Button'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const { setToken } = useAuth()
  const navigate = useNavigate()

  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await api.post('/api/auth/login', { username: username.trim(), password })
      if (res.status === 200 && res.data && res.data.token) {
        setToken(res.data.token)
        showToast({ message: 'Signed in', type: 'success' })
        // Role-based redirect
        let to = '/dashboard'
        try {
          const decoded = jwtDecode(res.data.token)
          const roles = Array.isArray(decoded.roles) ? decoded.roles.map(r => String(r).replace(/^ROLE_/, '')) : []
          if (roles.includes('GENERAL_USER')) to = '/'
          else if (roles.includes('FACULTY') || roles.includes('CLUB_ASSOCIATE') || roles.includes('ADMIN')) to = '/dashboard'
        } catch {}
        navigate(to)
      } else if (res.status === 401) {
        setError('Invalid password. Please try again.')
        showToast({ message: 'Invalid username or password', type: 'error' })
      } else if (res.status === 404) {
        setError('User not found.')
        showToast({ message: 'User not found', type: 'error' })
      } else {
        setError('Login failed. Please try again.')
        showToast({ message: 'Login failed', type: 'error' })
      }
    } catch (err) {
      if (err.response) {
        if (err.response.status === 401) setError('Invalid password. Please try again.')
        else if (err.response.status === 404) setError('User not found.')
        else setError('Login failed. Please try again.')
      } else {
        setError('Network error. Please check your connection.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="w-full flex justify-center">
      <Card className="w-full max-w-[480px] p-0 overflow-hidden">
        <div className="px-8 py-7 border-b border-[#1F2937] bg-gradient-to-b from-[#111827] to-[#0F172A]">
          <div className="text-sm font-semibold text-[#9CA3AF]">EventSphere</div>
          <div className="mt-1 text-2xl font-semibold text-[#E5E7EB]">Welcome back</div>
          <div className="text-sm text-[#9CA3AF]">Sign in to continue</div>
        </div>

        <div className="p-8">
          <form onSubmit={onSubmit} className="space-y-5">
            <div className="form-group">
              <label className="form-label">Username</label>
              <input
                type="text"
                className="form-input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter your username"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  className="form-input pr-12"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  required
                  aria-invalid={!!error}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="absolute right-2 top-[9px] px-2 py-1"
                  onClick={() => setShowPassword(v => !v)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? 'Hide' : 'Show'}
                </Button>
              </div>
            </div>

            {error && (
              <div className="alert alert-error">
                {error}
              </div>
            )}

            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? (
                <div className="flex items-center justify-center">
                  <div className="spinner mr-2"></div>
                  Signing in...
                </div>
              ) : (
                'Sign In'
              )}
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-[#9CA3AF]">
              New here? <Link to="/register" className="text-[#E5E7EB] hover:text-[#3B82F6] transition-colors">Create an account</Link>
            </p>
          </div>

          <div className="mt-6">
            <div className="rounded-2xl border border-[#1F2937] bg-[#0F172A] p-4">
              <div className="text-xs font-semibold text-[#E5E7EB]">Demo accounts</div>
              <div className="mt-2 grid grid-cols-1 gap-1 text-xs text-[#9CA3AF]">
                <div><span className="font-medium text-[#E5E7EB]">admin</span> / Admin@123</div>
                <div><span className="font-medium text-[#E5E7EB]">faculty</span> / Faculty@123</div>
                <div><span className="font-medium text-[#E5E7EB]">club</span> / Club@123</div>
                <div><span className="font-medium text-[#E5E7EB]">user</span> / User@123</div>
              </div>
            </div>
          </div>
        </div>
      </Card>
    </div>
  )
}
import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../lib/api'
import { showToast } from '../lib/toast'
import Card from '../ui/Card'
import Button from '../ui/Button'

export default function Register() {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [role, setRole] = useState('USER')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  const navigate = useNavigate()

  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setMessage('')
    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}$/
    if (!emailPattern.test(email.trim())) {
      setError('Please enter a valid email address.')
      showToast({ message: 'Please enter a valid email address', type: 'error' })
      return
    }
    if (password !== confirm) {
      setError('Passwords do not match.')
      showToast({ message: 'Passwords do not match', type: 'error' })
      return
    }
    try {
      setLoading(true)
      const res = await api.post('/api/auth/register', { username: username.trim(), email: email.trim(), password, role })
      if (res.status === 200) {
        const msg = typeof res.data === 'string' ? res.data : 'Registration successful. You can now sign in.'
        setMessage(msg)
        showToast({ message: 'Registration successful', type: 'success' })
        if (role === 'USER') {
          setTimeout(() => navigate('/login'), 800)
        }
      } else {
        setError('Registration failed. Please try again.')
        showToast({ message: 'Registration failed', type: 'error' })
      }
    } catch (err) {
      if (err.response && err.response.data) setError(String(err.response.data))
      else setError('Network error. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="w-full flex justify-center">
      <Card className="w-full max-w-[520px] p-0 overflow-hidden">
        <div className="px-8 py-7 border-b border-[#1F2937] bg-gradient-to-b from-[#111827] to-[#0F172A]">
          <div className="text-sm font-semibold text-[#9CA3AF]">EventSphere</div>
          <div className="mt-1 text-2xl font-semibold text-[#E5E7EB]">Create account</div>
          <div className="text-sm text-[#9CA3AF]">General users are registered instantly. Other roles need admin approval.</div>
        </div>

        <div className="p-8">
          <form onSubmit={onSubmit} className="space-y-5">
            <div className="form-group">
              <label className="form-label">Username</label>
              <input className="form-input" value={username} onChange={(e)=>setUsername(e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">Role</label>
              <select className="form-select" value={role} onChange={(e)=>setRole(e.target.value)}>
                <option value="USER">User</option>
                <option value="FACULTY">Faculty</option>
                <option value="CLUB_ASSOCIATE">Club Associate</option>
              </select>
              <div className="mt-2 text-xs text-[#9CA3AF]">
                Users can register instantly. Faculty and Club Associate roles require approval.
              </div>
            </div>
          <div className="form-group">
            <label className="form-label">Email</label>
            <input
              type="email"
              className="form-input"
              value={email}
              onChange={(e)=>setEmail(e.target.value)}
              pattern="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[A-Za-z]{2,}$"
              title="Please enter a valid email address (example@domain.com)"
              placeholder="name@domain.com"
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">Password</label>
            <input type="password" className="form-input" value={password} onChange={(e)=>setPassword(e.target.value)} placeholder="Create a password" required />
          </div>
          <div className="form-group">
            <label className="form-label">Confirm Password</label>
            <input type="password" className="form-input" value={confirm} onChange={(e)=>setConfirm(e.target.value)} placeholder="Repeat your password" required />
          </div>

            {error && <div className="alert alert-error" role="alert" aria-live="assertive">{error}</div>}
            {message && <div className="alert alert-success" role="status" aria-live="polite">{message}</div>}

            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? 'Creating account...' : 'Create Account'}
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-[#9CA3AF]">
              Already have an account? <Link to="/login" className="text-[#E5E7EB] hover:text-[#3B82F6] transition-colors">Sign in</Link>
            </p>
          </div>
        </div>
      </Card>
    </div>
  )
}

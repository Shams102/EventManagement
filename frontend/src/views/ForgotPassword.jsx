import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../lib/api'
import Card from '../ui/Card'
import Button from '../ui/Button'
import { showToast } from '../lib/toast'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')

  const onSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api.post('/api/auth/forgot-password', { email })
      setSuccess(true)
      showToast({ message: 'Reset link sent successfully', type: 'success' })
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to request password reset')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-[80vh] items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <Card className="w-full max-w-md p-8">
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold tracking-tight text-[#E5E7EB]">Reset Account Password</h2>
          <p className="mt-2 text-sm text-[#9CA3AF]">
            Enter your email address and we'll send you a link to reset your password.
          </p>
        </div>

        {success ? (
          <div className="text-center">
            <div className="alert alert-success mb-6">
              If an account exists for {email}, you will receive a reset link shortly.
              Please check your email.
            </div>
            <Link to="/login" className="text-sm font-medium text-[#3B82F6] hover:text-[#60A5FA]">
              Return to Login
            </Link>
          </div>
        ) : (
          <form className="space-y-6" onSubmit={onSubmit}>
            {error && <div className="alert alert-error">{error}</div>}
            
            <div className="form-group">
              <label htmlFor="email" className="form-label">Email address</label>
              <input
                id="email"
                name="email"
                type="email"
                required
                className="form-input"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div>
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? 'Sending...' : 'Send Reset Link'}
              </Button>
            </div>
            
            <div className="text-center text-sm">
              <Link to="/login" className="font-medium text-[#3B82F6] hover:text-[#60A5FA]">
                Back to Login
              </Link>
            </div>
          </form>
        )}
      </Card>
    </div>
  )
}

import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { jwtDecode } from 'jwt-decode'
import api, { logoutRequest } from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'))
  const [user, setUser] = useState(null)
  const [roles, setRoles] = useState([])
  const [clubId, setClubId] = useState(null)
  const [requestedRole, setRequestedRole] = useState(null)
  const [isApproved, setIsApproved] = useState(true)

  useEffect(() => {
    if (token) {
      localStorage.setItem('token', token)
      try {
        const decoded = jwtDecode(token)
        setUser(decoded)
        const r = Array.isArray(decoded.roles) ? decoded.roles : []
        setRoles(r.map(x => String(x).replace(/^ROLE_/, '')))
        setClubId(decoded.clubId || null)
        setRequestedRole(null)
        setIsApproved(true)
      } catch {
        setUser(null)
        setRoles([])
        setClubId(null)
        setRequestedRole(null)
        setIsApproved(true)
      }
    } else {
      localStorage.removeItem('token')
      setUser(null)
      setRoles([])
      setClubId(null)
      setRequestedRole(null)
      setIsApproved(true)
    }
  }, [token])

  useEffect(() => {
    if (!token) return
    let mounted = true
    api.get('/api/profile').then((res) => {
      if (!mounted) return
      setRequestedRole(res?.data?.requestedRole || null)
      setIsApproved(Boolean(res?.data?.isApproved ?? true))
    }).catch(() => {
      if (!mounted) return
      setRequestedRole(null)
      setIsApproved(true)
    })
    return () => { mounted = false }
  }, [token])

  const value = useMemo(() => ({
    token,
    setToken,
    user,
    roles,
    clubId,
    requestedRole,
    isApproved,
    hasRole: (r) => roles.includes(r) || roles.includes(String(r).toUpperCase()),
    logout: async () => {
      try {
        await logoutRequest()
      } catch {
        // ignore logout network failures
      } finally {
        setToken(null)
      }
    }
  }), [token, user, roles, clubId, requestedRole, isApproved])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() { return useContext(AuthContext) }



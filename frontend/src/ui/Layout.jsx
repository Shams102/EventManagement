import React, { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import NotificationBell from './notifications/NotificationBell'
import NotificationsDrawer from './notifications/NotificationsDrawer'
import BroadcastModal from './BroadcastModal'
import Container from './Container'
import Button from './Button'

export default function Layout({ children }) {
  const { user, logout, hasRole, requestedRole, isApproved } = useAuth()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [broadcastOpen, setBroadcastOpen] = useState(false)
  const [showPendingCard, setShowPendingCard] = useState(false)
  
  const isActive = (path) => location.pathname === path

  const links = useMemo(() => {
    const items = [
      { to: '/', label: 'Home', show: true },
      { to: '/dashboard', label: 'Dashboard', show: !!user },
      { to: '/events', label: 'Events', show: true },
      { to: '/book-room', label: 'Book Room', show: !!user && (hasRole('ADMIN') || hasRole('BUILDING_ADMIN') || hasRole('CENTRAL_ADMIN') || hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE')) },
      { to: '/admin/role-requests', label: 'Role Approval', show: !!user && (hasRole('ADMIN') || hasRole('CENTRAL_ADMIN')) },
      { to: '/admin/room-approvals', label: 'Room Approvals', show: !!user && (hasRole('ADMIN') || hasRole('BUILDING_ADMIN')) },
    ]
    return items.filter(i => i.show)
  }, [user, hasRole])

  const hasPendingRoleApproval = !!user
    && (hasRole('GENERAL_USER') || hasRole('USER'))
    && !!requestedRole
    && !isApproved

  useEffect(() => {
    setShowPendingCard(hasPendingRoleApproval)
  }, [hasPendingRoleApproval, user?.sub])

  useEffect(() => {
    if (!showPendingCard) return
    const t = setTimeout(() => setShowPendingCard(false), 5000)
    return () => clearTimeout(t)
  }, [showPendingCard])
  
  return (
    <div className="min-h-screen bg-[#0B0F19] text-[#E5E7EB]">
      {/* Header */}
      <header className="bg-[#0B0F19]/95 backdrop-blur border-b border-[#1F2937] sticky top-0">
        <Container className="py-3">
          <div className="flex items-center justify-between">
            {/* Logo */}
            <div className="flex items-center">
              <Link to="/" className="logo-link flex items-center gap-2 transition-all duration-200 ease hover:opacity-95">
                <img
                  src="/logo.png"
                  alt="EventSphere logo"
                  className="h-8 w-auto transition-transform duration-200 ease hover:scale-[1.05] drop-shadow-[0_6px_18px_rgba(59,130,246,0.18)]"
                />
                <span>EventSphere</span>
              </Link>
            </div>
            
            {/* Navigation */}
            <nav className="hidden md:flex items-center" role="navigation" aria-label="Primary navigation">
              {links.map(l => (
                <Link
                  key={l.to}
                  to={l.to}
                  className={`nav-link ${isActive(l.to) ? 'active' : ''}`}
                  aria-current={isActive(l.to) ? 'page' : undefined}
                >
                  {l.label}
                </Link>
              ))}
            </nav>
            
            {/* User Menu */}
            <div className="flex items-center space-x-4">
              <Button
                variant="secondary"
                size="sm"
                className="md:hidden"
                onClick={() => setMobileOpen(v => !v)}
                aria-label="Open menu"
              >
                Menu
              </Button>
              {hasRole('CENTRAL_ADMIN') && (
                <Button
                  variant="primary"
                  size="sm"
                  className="shadow-[0_10px_30px_rgba(0,0,0,0.35)]"
                  onClick={() => setBroadcastOpen(true)}
                  aria-label="Open broadcast dialog"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path d="M4 13.5v-3a2 2 0 0 1 2-2h2l8-4v15l-8-4H6a2 2 0 0 1-2-2Z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
                    <path d="M18 9.5a3.5 3.5 0 0 1 0 5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                    <path d="M20 7.5a6.5 6.5 0 0 1 0 9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  </svg>
                  Broadcast
                </Button>
              )}
              <NotificationBell open={notificationsOpen} onOpen={() => setNotificationsOpen(true)} />
              {user ? (
                <div className="flex items-center space-x-3">
                  <Link to="/profile" aria-label="Profile">
                    <Button variant="secondary" size="sm" className="gap-2">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <path d="M20 21a8 8 0 1 0-16 0" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                        <path d="M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" stroke="currentColor" strokeWidth="2" />
                      </svg>
                      <span className="hidden sm:inline">Profile</span>
                      <span className="sm:hidden">{user.sub}</span>
                    </Button>
                  </Link>
                  <Button 
                    onClick={logout}
                    variant="secondary"
                    size="sm"
                  >
                    Logout
                  </Button>
                </div>
              ) : (
                <div className="flex items-center space-x-2">
                  <Link to="/login">
                    <Button size="sm">Login</Button>
                  </Link>
                  <Link to="/register">
                    <Button variant="secondary" size="sm">Register</Button>
                  </Link>
                </div>
              )}
            </div>
            {/* Notifications drawer (shared) */}
            <NotificationsDrawer open={notificationsOpen} onClose={() => setNotificationsOpen(false)} />
            <BroadcastModal open={broadcastOpen} onClose={() => setBroadcastOpen(false)} />
          </div>
        </Container>

        {mobileOpen && (
          <div className="md:hidden border-t border-slate-800 bg-slate-950/95">
            <Container className="py-2">
              <nav className="flex flex-col">
                {links.map(l => (
                  <Link
                    key={l.to}
                    to={l.to}
                    className={`nav-link ${isActive(l.to) ? 'active' : ''}`}
                    onClick={() => setMobileOpen(false)}
                  >
                    {l.label}
                  </Link>
                ))}
              </nav>
            </Container>
          </div>
        )}
        
      </header>

      {showPendingCard && (
        <div className="fixed right-5 top-[80px] z-[9999] w-[min(360px,calc(100vw-2.5rem))] rounded-[10px] border border-yellow-500/30 bg-[#1E293B] px-4 py-3 text-sm text-yellow-300 shadow-[0_5px_20px_rgba(0,0,0,0.4)]">
          Logged in as General User.
          <br />
          Waiting for Role Approval.
        </div>
      )}
      
      {/* Main Content */}
      <main>
        <Container className="py-8">
          {children}
        </Container>
      </main>
      
      {/* Footer */}
      <footer className="bg-[#0B0F19] border-t border-[#1F2937] mt-12">
        <Container className="py-8">
          <div className="text-center text-slate-400">
            <p>&copy; 2024 EventSphere. Built with React &amp; Spring Boot.</p>
            <div className="mt-2 text-sm">
              <a href="/style-guide" className="nav-link">Style guide</a>
            </div>
          </div>
        </Container>
      </footer>
    </div>
  )
}



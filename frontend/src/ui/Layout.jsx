import React, { useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import NotificationBell from './notifications/NotificationBell'
import NotificationsDrawer from './notifications/NotificationsDrawer'
import BroadcastModal from './BroadcastModal'
import Toast from './Toast'
import Container from './Container'
import Button from './Button'

export default function Layout({ children }) {
  const { user, logout, hasRole } = useAuth()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [broadcastOpen, setBroadcastOpen] = useState(false)
  
  const isActive = (path) => location.pathname === path

  const links = useMemo(() => {
    const items = [
      { to: '/', label: 'Home', show: true },
      { to: '/dashboard', label: 'Dashboard', show: !!user },
      { to: '/events', label: 'Events', show: true },
      { to: '/bookings', label: 'Bookings', show: !!user && (hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('ADMIN')) },
      { to: '/enhanced-book-room', label: 'Book Room', show: !!user && (hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('ADMIN')) },
      { to: '/admin/role-requests', label: 'Admin', show: !!user && hasRole('ADMIN') },
      { to: '/admin/room-approvals', label: 'Room Approvals', show: !!user && hasRole('ADMIN') },
    ]
    return items.filter(i => i.show)
  }, [user, hasRole])
  
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {/* Header */}
      <header className="bg-slate-900/95 backdrop-blur border-b border-slate-800 sticky top-0">
        <Container className="py-3">
          <div className="flex items-center justify-between">
            {/* Logo */}
            <div className="flex items-center">
              <Link to="/" className="logo-link">
                🎓 EventSphere
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
              {hasRole('ADMIN') && (
                <button className="btn btn-ghost btn-sm" title="Broadcast" onClick={() => setBroadcastOpen(true)} aria-label="Open broadcast dialog">📣</button>
              )}
              <NotificationBell open={notificationsOpen} onOpen={() => setNotificationsOpen(true)} />
              {user ? (
                <div className="flex items-center space-x-3">
                  <span className="hidden sm:inline text-sm text-slate-300">Welcome, {user.sub}</span>
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
            <Toast />
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
      
      {/* Main Content */}
      <main>
        <Container className="py-8">
          {children}
        </Container>
      </main>
      
      {/* Footer */}
      <footer className="bg-slate-900 border-t border-slate-800 mt-12">
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



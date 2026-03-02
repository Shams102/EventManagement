import React, { useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import NotificationBell from './notifications/NotificationBell'
import NotificationsDrawer from './notifications/NotificationsDrawer'
import BroadcastModal from './BroadcastModal'
import Toast from './Toast'
import { Home, LayoutDashboard, Calendar, CalendarCheck, DoorOpen, ShieldCheck, CheckSquare } from 'lucide-react'

export default function Layout({ children }) {
  const { user, logout, hasRole } = useAuth()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [broadcastOpen, setBroadcastOpen] = useState(false)
  
  const isActive = (path) => location.pathname === path

  const links = useMemo(() => {
    const items = [
      { to: '/', label: 'Home', icon: Home, show: true },
      { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, show: !!user },
      { to: '/events', label: 'Events', icon: Calendar, show: true },
      { to: '/bookings', label: 'Bookings', icon: CalendarCheck, show: !!user && (hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('ADMIN')) },
      { to: '/enhanced-book-room', label: 'Book Room', icon: DoorOpen, show: !!user && (hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('ADMIN')) },
      { to: '/admin/role-requests', label: 'Admin', icon: ShieldCheck, show: !!user && hasRole('ADMIN') },
      { to: '/admin/room-approvals', label: 'Room Approvals', icon: CheckSquare, show: !!user && hasRole('ADMIN') },
    ]
    return items.filter(i => i.show)
  }, [user, hasRole])
  
  return (
    <div className="min-h-screen bg-slate-900 flex flex-col md:flex-row">
      {/* Sidebar (Desktop) / Header (Mobile) */}
      <header className="bg-slate-900/80 backdrop-blur-md shadow-sm border-b md:border-b-0 md:border-r border-slate-700 md:sticky md:top-0 md:h-screen md:w-64 md:flex-shrink-0 z-50">
        <div className="flex flex-col h-full">
          <div className="flex items-center justify-between h-16 px-4 md:px-6">
            {/* Logo */}
            <div className="flex items-center">
              <Link to="/" className="inline-flex items-center gap-2 font-bold text-violet-400 text-lg">
                🎓 EventSphere
              </Link>
            </div>
            
            {/* Mobile Menu Button */}
            <div className="flex items-center md:hidden space-x-4">
              <div className="text-slate-300">
                <NotificationBell open={notificationsOpen} onOpen={() => setNotificationsOpen(true)} />
              </div>
              <button
                type="button"
                className="px-3 py-1.5 bg-slate-800 text-slate-200 rounded-md text-sm border border-slate-700"
                onClick={() => setMobileOpen(v => !v)}
                aria-label="Open menu"
              >
                Menu
              </button>
            </div>
          </div>

          {/* Desktop Navigation / User Info */}
          <div className={`flex-1 overflow-y-auto ${mobileOpen ? 'block' : 'hidden'} md:block px-4 py-4 md:px-6`}>
            <nav className="flex flex-col space-y-2 mb-8" role="navigation" aria-label="Primary navigation">
              {links.map(l => {
                const Icon = l.icon
                return (
                  <Link
                    key={l.to}
                    to={l.to}
                    className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                      isActive(l.to)
                        ? 'bg-violet-600/20 text-violet-300 border border-violet-500/20 shadow-[0_0_10px_rgba(139,92,246,0.1)]'
                        : 'text-slate-400 hover:bg-slate-800 hover:text-slate-100 border border-transparent'
                    }`}
                    onClick={() => setMobileOpen(false)}
                    aria-current={isActive(l.to) ? 'page' : undefined}
                  >
                    {Icon && <Icon className="w-5 h-5" />}
                    {l.label}
                  </Link>
                )
              })}
            </nav>

            {/* User Menu / Bottom Actions */}
            <div className="pt-6 border-t border-slate-700/50 flex flex-col space-y-4">
              <div className="hidden md:flex items-center justify-between">
                <span className="text-sm text-slate-400">Notifications</span>
                <div className="text-slate-300">
                  <NotificationBell open={notificationsOpen} onOpen={() => setNotificationsOpen(true)} />
                </div>
              </div>

              {hasRole('ADMIN') && (
                <button className="flex items-center gap-3 px-3 py-2 text-sm text-slate-400 hover:bg-slate-800 hover:text-slate-100 rounded-lg transition-colors border border-transparent" title="Broadcast" onClick={() => setBroadcastOpen(true)} aria-label="Open broadcast dialog">
                  📣 Broadcast
                </button>
              )}

              {user ? (
                <div className="flex flex-col space-y-3 pt-2">
                  <span className="text-sm font-medium text-slate-300 break-all px-1">👤 {user.sub}</span>
                  <button 
                    onClick={logout}
                    className="w-full text-center px-4 py-2 bg-slate-800/80 text-slate-200 hover:bg-slate-700 rounded-lg text-sm font-medium transition-colors border border-slate-600/50"
                  >
                    Logout
                  </button>
                </div>
              ) : (
                <div className="flex flex-col space-y-2">
                  <Link to="/login" className="w-full text-center px-4 py-2 bg-violet-600 text-white hover:bg-violet-700 rounded-lg text-sm font-medium transition-all shadow-[0_0_15px_rgba(124,58,237,0.3)] border border-violet-500/50">Login</Link>
                  <Link to="/register" className="w-full text-center px-4 py-2 bg-slate-800/80 text-slate-200 hover:bg-slate-700 rounded-lg text-sm font-medium transition-colors border border-slate-600/50">Register</Link>
                </div>
              )}
            </div>
          </div>

          {/* Notifications drawer (shared) */}
          <NotificationsDrawer open={notificationsOpen} onClose={() => setNotificationsOpen(false)} />
          <BroadcastModal open={broadcastOpen} onClose={() => setBroadcastOpen(false)} />
          <Toast />
        </div>
      </header>
      
      {/* Main Content */}
      <div className="flex-1 flex flex-col min-h-screen relative overflow-x-hidden">
        <main className="flex-1 p-4 md:p-8 lg:p-10 w-full max-w-7xl mx-auto">
          {children}
        </main>

        {/* Footer */}
        <footer className="mt-auto border-t border-slate-800/60 bg-slate-900/50 backdrop-blur-sm">
          <div className="px-6 py-6 max-w-7xl mx-auto">
            <div className="flex flex-col sm:flex-row items-center justify-between text-slate-500 text-sm">
              <p>&copy; 2024 EventSphere.</p>
              <div className="mt-2 sm:mt-0">
                <a href="/style-guide" className="hover:text-violet-400 transition-colors">Style guide</a>
              </div>
            </div>
          </div>
        </footer>
      </div>
    </div>
  )
}



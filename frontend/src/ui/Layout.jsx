import React, { useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import NotificationBell from './notifications/NotificationBell'
import NotificationsDrawer from './notifications/NotificationsDrawer'
import BroadcastModal from './BroadcastModal'
import Toast from './Toast'
import { Home, LayoutDashboard, Calendar, Users, Settings, BookOpen, DoorOpen, ShieldCheck, ListChecks, Menu, X } from 'lucide-react'

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
      { to: '/bookings', label: 'Bookings', icon: BookOpen, show: !!user && (hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('ADMIN')) },
      { to: '/enhanced-book-room', label: 'Book Room', icon: DoorOpen, show: !!user && (hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE') || hasRole('ADMIN')) },
      { to: '/admin/role-requests', label: 'Users', icon: Users, show: !!user && hasRole('ADMIN') },
      { to: '/admin/room-approvals', label: 'Settings', icon: Settings, show: !!user && hasRole('ADMIN') },
    ]
    return items.filter(i => i.show)
  }, [user, hasRole])
  
  return (
    <div className="min-h-screen bg-[#0b0f19] flex flex-col md:flex-row text-slate-200">
      {/* Sidebar (Desktop) / Header (Mobile) */}
      <header className="bg-[#1e293b]/80 backdrop-blur-md border-b md:border-b-0 md:border-r border-slate-700 md:w-64 md:flex-shrink-0 z-50 md:sticky md:top-0 md:h-screen transition-all shadow-lg">
        <div className="flex flex-col h-full">
          <div className="flex items-center justify-between h-16 px-4 md:px-6 border-b border-slate-700/50">
            {/* Logo */}
            <Link to="/" className="inline-flex items-center gap-2 font-bold text-violet-400 text-xl tracking-tight">
              <span className="text-2xl">🎓</span> EventSphere
            </Link>
            
            {/* Mobile Menu Button */}
            <div className="flex items-center md:hidden space-x-3">
              <div className="text-slate-300">
                <NotificationBell open={notificationsOpen} onOpen={() => setNotificationsOpen(true)} />
              </div>
              <button
                type="button"
                className="p-1.5 text-slate-300 hover:text-white rounded-md bg-slate-800 border border-slate-700"
                onClick={() => setMobileOpen(v => !v)}
                aria-label="Toggle menu"
              >
                {mobileOpen ? <X size={20} /> : <Menu size={20} />}
              </button>
            </div>
          </div>

          {/* Navigation Area */}
          <div className={`flex-1 overflow-y-auto py-4 px-3 md:px-4 flex flex-col ${mobileOpen ? 'block' : 'hidden md:flex'}`}>
            <nav className="flex-1 space-y-1.5" role="navigation" aria-label="Primary navigation">
              {links.map(l => {
                const Icon = l.icon
                return (
                  <Link
                    key={l.to}
                    to={l.to}
                    className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200 ${
                      isActive(l.to)
                        ? 'bg-violet-600/20 text-violet-300 border border-violet-500/30 shadow-[0_0_15px_rgba(139,92,246,0.15)]'
                        : 'text-slate-400 hover:bg-slate-800/80 hover:text-slate-200 border border-transparent'
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

            {/* Bottom Actions */}
            <div className="mt-6 pt-6 border-t border-slate-700/50 space-y-4">
              <div className="hidden md:flex items-center justify-between px-2">
                <span className="text-sm font-medium text-slate-400">Notifications</span>
                <div className="text-slate-300 hover:text-white transition-colors">
                  <NotificationBell open={notificationsOpen} onOpen={() => setNotificationsOpen(true)} />
                </div>
              </div>

              {hasRole('ADMIN') && (
                <button
                  className="flex items-center gap-3 w-full px-3 py-2.5 text-sm font-medium text-slate-400 hover:bg-slate-800/80 hover:text-slate-200 rounded-lg transition-colors"
                  title="Broadcast"
                  onClick={() => setBroadcastOpen(true)}
                >
                  <span className="text-lg">📣</span> Broadcast
                </button>
              )}

              <div className="pt-2">
                {user ? (
                  <div className="flex flex-col space-y-3 bg-slate-900/50 p-3 rounded-xl border border-slate-800">
                    <div className="flex items-center gap-2 px-1">
                      <div className="w-8 h-8 rounded-full bg-violet-900 text-violet-300 flex items-center justify-center font-bold text-xs shrink-0 border border-violet-700">
                        {user.sub?.charAt(0).toUpperCase()}
                      </div>
                      <span className="text-xs font-medium text-slate-300 truncate" title={user.sub}>{user.sub}</span>
                    </div>
                    <button
                      onClick={logout}
                      className="w-full py-2 bg-slate-800 text-slate-300 hover:bg-red-500/20 hover:text-red-300 rounded-lg text-xs font-bold transition-all border border-slate-700 hover:border-red-500/30"
                    >
                      LOGOUT
                    </button>
                  </div>
                ) : (
                  <div className="flex flex-col space-y-2">
                    <Link to="/login" className="w-full text-center py-2.5 bg-violet-600 hover:bg-violet-500 text-white rounded-lg text-sm font-bold transition-all shadow-[0_0_20px_rgba(124,58,237,0.3)]">
                      Log In
                    </Link>
                    <Link to="/register" className="w-full text-center py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-sm font-bold transition-all border border-slate-700">
                      Sign Up
                    </Link>
                  </div>
                )}
              </div>
            </div>
          </div>

          <NotificationsDrawer open={notificationsOpen} onClose={() => setNotificationsOpen(false)} />
          <BroadcastModal open={broadcastOpen} onClose={() => setBroadcastOpen(false)} />
          <Toast />
        </div>
      </header>
      
      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-h-screen overflow-x-hidden relative">
        <main className="flex-1 p-4 md:p-8 lg:p-10 w-full max-w-6xl mx-auto">
          {children}
        </main>

        {/* Footer */}
        <footer className="mt-auto border-t border-slate-800 bg-[#1e293b]/50 backdrop-blur-sm py-6">
          <div className="px-6 w-full max-w-6xl mx-auto flex flex-col sm:flex-row items-center justify-between text-slate-500 text-sm">
            <p>&copy; 2024 EventSphere.</p>
            <div className="mt-2 sm:mt-0">
              <Link to="/style-guide" className="hover:text-violet-400 transition-colors">Style Guide</Link>
            </div>
          </div>
        </footer>
      </div>
    </div>
  )
}



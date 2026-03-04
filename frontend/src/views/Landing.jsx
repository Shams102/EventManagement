import React from 'react'
import { Link } from 'react-router-dom'
import Card from '../ui/Card'
import Button from '../ui/Button'

export default function Landing() {
  return (
    <div>
      {/* Hero Section */}
      <div className="mb-12">
        <Card className="overflow-hidden p-0 bg-slate-900">
          <div className="px-8 py-10 md:p-12">
            <div className="inline-flex items-center gap-2 rounded-full border border-slate-700 bg-slate-800/80 px-3 py-1 text-xs font-semibold text-slate-300">
              <span className="text-blue-400">New</span>
              <span className="text-slate-400">Modern campus event &amp; room booking hub</span>
            </div>

            <div className="mt-5 text-4xl md:text-6xl font-bold text-slate-100 leading-tight">
              Run better campus events with
              {' '}
              <span className="text-blue-400">clarity</span>
              {' '}
              and
              {' '}
              <span className="text-emerald-400">confidence</span>.
            </div>

            <p className="mt-4 text-lg text-slate-300 max-w-2xl">
              Discover events, register in seconds, and manage room requests with fewer conflicts and a cleaner workflow.
            </p>

            <div className="mt-6 flex flex-col sm:flex-row gap-3">
              <Link to="/events">
                <Button size="lg">
                  Browse Events
                </Button>
              </Link>
              <Link to="/book-room">
                <Button variant="secondary" size="lg">
                  Book a Room
                </Button>
              </Link>
            </div>

            <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-4">
              <Card className="p-4">
                <div className="text-sm font-semibold text-slate-100">Calendar-first discovery</div>
                <div className="mt-1 text-sm text-slate-400">A quick view of what’s happening this month.</div>
              </Card>
              <Card className="p-4">
                <div className="text-sm font-semibold text-slate-100">Registration schemas</div>
                <div className="mt-1 text-sm text-slate-400">Collect only what you need from attendees.</div>
              </Card>
              <Card className="p-4">
                <div className="text-sm font-semibold text-slate-100">Room conflict protection</div>
                <div className="mt-1 text-sm text-slate-400">Availability checks that match real booking rules.</div>
              </Card>
            </div>
          </div>
        </Card>
      </div>

      {/* Features Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
        <Card>
          <div className="h-12 w-12 rounded-xl flex items-center justify-center" style={{ background: 'rgba(37,99,235,0.35)' }}>
            <div className="text-2xl">📅</div>
          </div>
          <h3 className="text-xl font-semibold mt-4 text-slate-100">Event Discovery</h3>
          <p className="text-slate-400 mt-2">
            Browse upcoming events with an interactive calendar. Find workshops, lectures, and social events happening around campus.
          </p>
        </Card>
        
        <Card>
          <div className="h-12 w-12 rounded-xl flex items-center justify-center" style={{ background: 'rgba(16,185,129,0.35)' }}>
            <div className="text-2xl">🏢</div>
          </div>
          <h3 className="text-xl font-semibold mt-4 text-slate-100">Room Booking</h3>
          <p className="text-slate-400 mt-2">
            Book conference rooms, lecture halls, and meeting spaces. Check availability and avoid clashes.
          </p>
        </Card>
        
        <Card>
          <div className="h-12 w-12 rounded-xl flex items-center justify-center" style={{ background: 'rgba(245,158,11,0.35)' }}>
            <div className="text-2xl">👥</div>
          </div>
          <h3 className="text-xl font-semibold mt-4 text-slate-100">Community</h3>
          <p className="text-slate-400 mt-2">
            Engage students, faculty, and staff. Register for events and stay on top of updates.
          </p>
        </Card>
      </div>

    </div>
  )
}



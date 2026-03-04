import React from 'react'
import { motion } from 'framer-motion'

const getAccentColor = (category) => {
  const key = String(category || '').toLowerCase()
  if (key.includes('tech') || key.includes('engineering')) return '#3b82f6' // blue
  if (key.includes('cult') || key.includes('cultural')) return '#22c55e' // green
  if (key.includes('admin') || key.includes('office')) return '#ef4444' // red
  if (key.includes('sports')) return '#f59e0b' // amber
  return '#6366f1' // default indigo
}

export default function EventCalendarEvent({ title, timeText, category }) {
  const accent = getAccentColor(category)

  return (
    <motion.div
      className="calendar-event calendar-event-animate-in bg-slate-800 border border-slate-700 rounded-md px-3 py-2 hover:bg-slate-700 hover:border-slate-600 hover:scale-[1.02] transition-all duration-200 cursor-pointer flex items-start gap-2"
      whileHover={{ y: -1 }}
      whileTap={{ scale: 0.96 }}
      transition={{ type: 'spring', stiffness: 260, damping: 20, mass: 0.6 }}
    >
      <div
        className="w-1.5 h-6 rounded-full mt-0.5 shrink-0"
        style={{ backgroundColor: accent }}
      />
      <div className="flex-1 min-w-0">
        {timeText && (
          <div className="text-[11px] font-medium text-slate-300 leading-tight">
            {timeText}
          </div>
        )}
        <div className="text-[12px] font-semibold text-slate-100 leading-snug truncate">
          {title}
        </div>
      </div>
    </motion.div>
  )
}


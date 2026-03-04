import React from 'react'
import { AnimatePresence, motion } from 'framer-motion'

export default function EventPreviewModal({ isOpen, event, eligibilityText, onClose, onRegister }) {
  return (
    <AnimatePresence>
      {isOpen && event && (
        <motion.div
          className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
        >
          <motion.div
            className="bg-slate-900 border border-slate-800 rounded-xl shadow-2xl p-6 max-w-lg w-full"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.18 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-4">
              <h2 className="text-xl font-semibold text-slate-100">
                {event.title}
              </h2>
            </div>

            <div className="space-y-3 text-sm">
              <div className="text-slate-300">
                <div className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1">
                  Date &amp; time
                </div>
                <div>
                  {(() => {
                    const start = event.startTime || event.start
                    const end = event.endTime || event.end
                    if (!start) return 'TBD'
                    const startDate = new Date(start)
                    const endDate = end ? new Date(end) : null
                    return (
                      <>
                        <div>{startDate.toLocaleDateString()}</div>
                        <div>
                          {startDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          {endDate && (
                            <>
                              {' \u2013 '}
                              {endDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </>
                          )}
                        </div>
                      </>
                    )
                  })()}
                </div>
              </div>

              <div className="text-slate-300">
                <div className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1">
                  Location
                </div>
                <div>{event.location || 'TBD'}</div>
              </div>

              {event.description && (
                <div className="text-slate-300">
                  <div className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1">
                    Description
                  </div>
                  <p className="text-sm leading-relaxed">
                    {event.description}
                  </p>
                </div>
              )}

              {eligibilityText && (
                <div className="text-slate-300">
                  <div className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1">
                    Eligibility / allowed roles
                  </div>
                  <p className="text-sm leading-relaxed">
                    {eligibilityText}
                  </p>
                </div>
              )}
            </div>

            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                onClick={onClose}
                className="bg-slate-800 hover:bg-slate-700 text-slate-200 px-4 py-2 rounded-lg text-sm font-medium"
              >
                Close
              </button>
              <button
                type="button"
                onClick={onRegister}
                className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg text-sm font-semibold"
              >
                Register
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}


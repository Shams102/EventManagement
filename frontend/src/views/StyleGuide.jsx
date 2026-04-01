import React from 'react'
import { colors, spacing, typography } from '../ui/design-system'
import BroadcastForm from '../ui/BroadcastForm'
import Skeleton from '../ui/Skeleton'

export default function StyleGuide() {
  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold mb-4">Style guide — tokens & components</h1>
      <div className="card mb-4">
      <h2 className="font-semibold mb-2">Colors (sample)</h2>
        <div className="flex gap-3 flex-wrap">
          <div style={{background: colors.primary[500], width:120, height:60, borderRadius:8}}></div>
          <div style={{background: colors.secondary[500], width:120, height:60, borderRadius:8}}></div>
          <div style={{background: colors.success[500], width:120, height:60, borderRadius:8}}></div>
          <div style={{background: colors.warning[500], width:120, height:60, borderRadius:8}}></div>
          <div style={{background: colors.error[500], width:120, height:60, borderRadius:8}}></div>
        </div>
      </div>

      <div className="card mb-4">
      <h2 className="font-semibold mb-2">Buttons</h2>
        <div className="flex gap-3 mb-3">
          <button className="btn btn-primary">Primary</button>
          <button className="btn btn-secondary">Secondary</button>
          <button className="btn btn-success">Success</button>
          <button className="btn btn-danger">Danger</button>
        </div>
        <div className="flex gap-3">
          <button className="btn btn-primary btn-sm">Small</button>
          <button className="btn btn-primary btn-lg">Large</button>
        </div>
      </div>

      <div className="card mb-4">
      <h2 className="font-semibold mb-2">Form inputs</h2>
        <div className="space-y-3">
          <div>
            <label className="form-label">Text input</label>
            <input className="form-input" placeholder="Type here" />
          </div>
          <div>
            <label className="form-label">Select</label>
            <select aria-label="Example select" className="form-select"><option>Option 1</option><option>Option 2</option></select>
          </div>
        </div>
      </div>

      <div className="card mb-4">
      <h2 className="font-semibold mb-2">Broadcast form (reusable)</h2>
        <BroadcastForm />
      </div>

      <div className="card mb-4">
      <h2 className="font-semibold mb-2">Skeletons</h2>
        <div className="space-y-2">
          <Skeleton className="w-full" height="1.5rem" />
          <Skeleton className="w-full" height="1rem" />
          <div className="flex gap-2">
            <Skeleton className="w-32" height="2rem" />
            <Skeleton className="w-32" height="2rem" />
          </div>
        </div>
      </div>
    </div>
  )
}

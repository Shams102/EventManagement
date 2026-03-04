import React from 'react'
import { cn } from './design-system'

export default function Card({ className, children, as: Component = 'div', ...props }) {
  return (
    <Component
      className={cn(
        'bg-slate-900 border border-slate-800 rounded-xl shadow-lg shadow-black/20 transition-all duration-200 hover:border-slate-700',
        className
      )}
      {...props}
    >
      {children}
    </Component>
  )
}


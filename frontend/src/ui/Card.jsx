import React from 'react'
import { cn } from './design-system'

export default function Card({ className, children, as: Component = 'div', ...props }) {
  return (
    <Component
      className={cn(
        'bg-[#111827] border border-[#1F2937] rounded-2xl shadow-[0_10px_30px_rgba(0,0,0,0.5)] transition-all duration-200',
        className
      )}
      {...props}
    >
      {children}
    </Component>
  )
}


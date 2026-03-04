import React from 'react'
import { cn } from './design-system'

export default function Container({ className, children }) {
  return (
    <div className={cn('max-w-7xl mx-auto px-6 py-12', className)}>
      {children}
    </div>
  )
}


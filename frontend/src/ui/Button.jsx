import React from 'react'
import { cn } from './design-system'

const baseClasses =
  'inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-all duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 disabled:opacity-60 disabled:cursor-not-allowed'

const sizeClasses = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-4 py-2 text-sm',
  lg: 'px-5 py-2.5 text-base',
}

const variantClasses = {
  primary: 'bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-black/20',
  secondary: 'bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-100',
  ghost: 'bg-transparent hover:bg-slate-800 text-slate-300',
  outline: 'border border-slate-700 text-slate-100 bg-transparent hover:bg-slate-800',
}

export default function Button({
  variant = 'primary',
  size = 'md',
  className,
  type = 'button',
  ...props
}) {
  return (
    <button
      type={type}
      className={cn(
        baseClasses,
        sizeClasses[size] || sizeClasses.md,
        variantClasses[variant] || variantClasses.primary,
        className
      )}
      {...props}
    />
  )
}


import React from 'react'
import { cn } from './design-system'

const baseClasses =
  'inline-flex items-center justify-center gap-2 font-medium transition-all duration-200 focus:outline-none disabled:opacity-60 disabled:cursor-not-allowed'

const sizeClasses = {
  sm: 'px-[10px] py-2 text-xs rounded-lg',
  md: 'px-4 py-[10px] text-sm rounded-[10px]',
  lg: 'px-5 py-3 text-base rounded-xl',
}

const variantClasses = {
  primary: 'bg-[#3B82F6] hover:bg-[#2563EB] text-white border border-transparent hover:scale-[1.02]',
  secondary: 'bg-transparent border border-[#374151] text-[#D1D5DB] hover:bg-white/5 hover:scale-[1.02]',
  success: 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-black/20',
  danger: 'bg-red-600 hover:bg-red-500 text-white shadow-lg shadow-black/20',
  ghost: 'bg-transparent text-[#9CA3AF] hover:text-[#E5E7EB] hover:bg-white/5',
  outline: 'border border-[#374151] text-[#E5E7EB] bg-transparent hover:bg-white/5 hover:scale-[1.02]',
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
        'focus-visible:ring-2 focus-visible:ring-[#3B82F6]/30 focus-visible:border-[#3B82F6]',
        className
      )}
      {...props}
    />
  )
}


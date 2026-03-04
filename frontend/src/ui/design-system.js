// Design System for Event Management UI
export const colors = {
  primary: {
    50: '#eff6ff',
    100: '#dbeafe',
    200: '#bfdbfe',
    300: '#93c5fd',
    400: '#60a5fa',
    500: '#3b82f6',
    600: '#2563eb',
    700: '#1d4ed8',
    800: '#1e40af',
    900: '#1e3a8a',
  },
  secondary: {
    50: '#f8fafc',
    100: '#f1f5f9',
    200: '#e2e8f0',
    300: '#cbd5e1',
    400: '#94a3b8',
    500: '#64748b',
    600: '#475569',
    700: '#334155',
    800: '#1e293b',
    900: '#0f172a',
  },
  success: {
    50: '#f0fdf4',
    100: '#dcfce7',
    200: '#bbf7d0',
    300: '#86efac',
    400: '#4ade80',
    500: '#22c55e',
    600: '#16a34a',
    700: '#15803d',
    800: '#166534',
    900: '#14532d',
  },
  warning: {
    50: '#fffbeb',
    100: '#fef3c7',
    200: '#fde68a',
    300: '#fcd34d',
    400: '#fbbf24',
    500: '#f59e0b',
    600: '#d97706',
    700: '#b45309',
    800: '#92400e',
    900: '#78350f',
  },
  error: {
    50: '#fef2f2',
    100: '#fee2e2',
    200: '#fecaca',
    300: '#fca5a5',
    400: '#f87171',
    500: '#ef4444',
    600: '#dc2626',
    700: '#b91c1c',
    800: '#991b1b',
    900: '#7f1d1d',
  },
  gray: {
    50: '#f9fafb',
    100: '#f3f4f6',
    200: '#e5e7eb',
    300: '#d1d5db',
    400: '#9ca3af',
    500: '#6b7280',
    600: '#4b5563',
    700: '#374151',
    800: '#1f2937',
    900: '#111827',
  },
}

export const spacing = {
  xs: '0.25rem',
  sm: '0.5rem',
  md: '1rem',
  lg: '1.5rem',
  xl: '2rem',
  '2xl': '3rem',
  '3xl': '4rem',
}

export const borderRadius = {
  sm: '0.5rem',
  md: '0.75rem',
  lg: '1rem',
  xl: '1.25rem',
  full: '9999px',
}

export const shadows = {
  sm: '0 2px 6px rgba(2,6,23,0.04)',
  md: '0 8px 20px rgba(2,6,23,0.06)',
  lg: '0 20px 40px rgba(2,6,23,0.08)',
  xl: '0 30px 60px rgba(2,6,23,0.09)',
}

export const typography = {
  fontFamily: {
    sans: ['Inter', 'system-ui', 'sans-serif'],
    mono: ['JetBrains Mono', 'Consolas', 'monospace'],
  },
  fontSize: {
    xs: ['0.75rem', { lineHeight: '1rem' }],
    sm: ['0.875rem', { lineHeight: '1.25rem' }],
    base: ['1rem', { lineHeight: '1.5rem' }],
    lg: ['1.125rem', { lineHeight: '1.75rem' }],
    xl: ['1.25rem', { lineHeight: '1.75rem' }],
    '2xl': ['1.5rem', { lineHeight: '2rem' }],
    '3xl': ['1.875rem', { lineHeight: '2.25rem' }],
    '4xl': ['2.25rem', { lineHeight: '2.5rem' }],
  },
}

export const breakpoints = {
  sm: '640px',
  md: '768px',
  lg: '1024px',
  xl: '1280px',
  '2xl': '1536px',
}

// Reusable component styles
export const buttonStyles = {
  base: 'inline-flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-lg focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 focus:ring-offset-slate-950 transition-all duration-200',
  sizes: {
    sm: 'px-3 py-1.5 text-xs',
    md: 'px-4 py-2 text-sm',
    lg: 'px-5 py-2.5 text-base',
  },
  variants: {
    primary: `bg-blue-600 text-white hover:bg-blue-500`,
    secondary: `bg-slate-800 text-slate-100 border border-slate-700 hover:bg-slate-700`,
    success: `bg-green-600 text-white hover:bg-green-500`,
    warning: `bg-yellow-600 text-white hover:bg-yellow-500`,
    error: `bg-red-600 text-white hover:bg-red-500`,
    outline: `border border-slate-700 text-slate-100 bg-transparent hover:bg-slate-800`,
    ghost: `text-slate-300 bg-transparent hover:bg-slate-800`,
  },
}

export const cardStyles = {
  base: 'bg-slate-900 rounded-xl shadow-lg shadow-black/20 border border-slate-800',
  padding: {
    sm: 'p-4',
    md: 'p-6',
    lg: 'p-8',
  },
}

export const inputStyles = {
  base: 'block w-full px-3 py-2 border border-slate-700 rounded-md bg-slate-900 text-slate-100 shadow-sm placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/60 focus:border-blue-500 sm:text-sm',
  error: 'border-red-500 text-red-200 placeholder-red-400 focus:ring-red-500 focus:border-red-500',
}

export const alertStyles = {
  base: 'p-4 rounded-lg border',
  variants: {
    info: 'bg-slate-900/80 border-slate-700 text-slate-100',
    success: 'bg-emerald-900/60 border-emerald-700 text-emerald-100',
    warning: 'bg-amber-900/60 border-amber-700 text-amber-100',
    error: 'bg-red-900/60 border-red-700 text-red-100',
  },
}

export const badgeStyles = {
  base: 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
  variants: {
    primary: 'bg-blue-600/20 text-blue-300',
    secondary: 'bg-slate-800 text-slate-200',
    success: 'bg-emerald-600/20 text-emerald-300',
    warning: 'bg-amber-600/20 text-amber-300',
    error: 'bg-red-600/20 text-red-300',
  },
}

export const sidebarStyles = {
  base: 'fixed inset-y-0 left-0 z-50 w-64 bg-slate-950 border-r border-slate-800 shadow-lg shadow-black/20 transform transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:inset-0',
  item: 'flex items-center px-4 py-2 text-sm font-medium rounded-md transition-colors duration-200',
  itemActive: 'bg-slate-800 text-slate-100 border-r-2 border-blue-500',
  itemInactive: 'text-slate-400 hover:bg-slate-800 hover:text-slate-100',
}

export const headerStyles = {
  base: 'bg-slate-900 border-b border-slate-800',
  title: 'text-lg font-semibold text-slate-100',
  subtitle: 'text-sm text-slate-400',
}

export const tableStyles = {
  base: 'min-w-full divide-y divide-slate-800',
  header: 'bg-slate-900',
  headerCell: 'px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider',
  bodyCell: 'px-6 py-4 whitespace-nowrap text-sm text-slate-100',
  row: 'hover:bg-slate-900/60',
}

export const paginationStyles = {
  base: 'flex items-center justify-between bg-slate-900 px-4 py-3 sm:px-6 border-t border-slate-800 sm:rounded-b-lg',
  button: 'relative inline-flex items-center px-4 py-2 border border-slate-700 text-sm font-medium rounded-md text-slate-200 bg-slate-900 hover:bg-slate-800',
  buttonActive: 'relative inline-flex items-center px-4 py-2 border border-blue-500 text-sm font-medium rounded-md text-white bg-blue-600',
}

export const loadingSpinner = 'spinner'
export const loadingDots = ['.', '.', '.']


// Utility functions
export const cn = (...classes) => {
  return classes.filter(Boolean).join(' ')
}

export const formatDate = (date) => {
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(date))
}

export const formatRelativeTime = (date) => {
  const now = new Date()
  const target = new Date(date)
  const diffMs = now - target
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return `${diffMins}m ago`
  if (diffHours < 24) return `${diffHours}h ago`
  if (diffDays < 7) return `${diffDays}d ago`
  return formatDate(date)
}

export const truncateText = (text, maxLength) => {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}

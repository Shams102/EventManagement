import { useEffect } from 'react'

export default function useClickOutside(ref, handler, when = true) {
  useEffect(() => {
    if (!when) return

    const onMouseDown = (event) => {
      const el = ref?.current
      if (!el) return
      if (el.contains(event.target)) return
      handler?.(event)
    }

    document.addEventListener('mousedown', onMouseDown)
    return () => document.removeEventListener('mousedown', onMouseDown)
  }, [ref, handler, when])
}


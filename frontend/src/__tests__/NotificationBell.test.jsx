import React from 'react'

import { render, screen, waitFor } from '@testing-library/react'
import NotificationBell from '../ui/notifications/NotificationBell'

vi.mock('../../lib/api', () => ({
  fetchInbox: vi.fn(() => Promise.resolve({ data: [] }))
}))

describe('NotificationBell', () => {
  it('renders bell icon', async () => {
    render(<NotificationBell />)
    expect(screen.getByRole('button')).toBeInTheDocument()
  })
})

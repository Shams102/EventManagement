import React from 'react'

import { render, screen } from '@testing-library/react'
import NotificationsDrawer from '../ui/notifications/NotificationsDrawer'

vi.mock('../../lib/api', () => ({
  fetchInbox: vi.fn(() => Promise.resolve({ data: [] }))
}))

describe('NotificationsDrawer', () => {
  it('renders nothing when not open', () => {
    render(<NotificationsDrawer open={false} onClose={() => {}} />)
    expect(screen.queryByText('Notifications')).not.toBeInTheDocument()
  })
  
  it('renders inbox when open', () => {
    render(<NotificationsDrawer open={true} onClose={() => {}} />)
    expect(screen.getByText('Notifications')).toBeInTheDocument()
  })
})

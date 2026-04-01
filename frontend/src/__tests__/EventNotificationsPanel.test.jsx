import React from 'react'

import { render, screen } from '@testing-library/react'
import EventNotificationsPanel from '../views/EventNotificationsPanel'

vi.mock('../lib/api', () => ({
  fetchEventNotifications: vi.fn(() => Promise.resolve({ data: [] }))
}))

import { BrowserRouter } from 'react-router-dom'

describe('EventNotificationsPanel', () => {
  it('renders loading or empty state', () => {
    render(<BrowserRouter><EventNotificationsPanel eventId={1} canView={true} canPost={false} /></BrowserRouter>)
    expect(screen.getByText('Event Notifications')).toBeInTheDocument()
  })
})

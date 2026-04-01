import React from 'react'

import { render, screen } from '@testing-library/react'
import NotificationCard from '../ui/notifications/NotificationCard'

describe('NotificationCard', () => {
  it('renders notification title and message', () => {
    const notif = { title: 'Test Title', message: 'Test Message', createdAt: new Date().toISOString(), urgency: 'NORMAL' }
    render(<NotificationCard item={notif} />)
    expect(screen.getByText('Test Title')).toBeInTheDocument()
    expect(screen.getByText('Test Message')).toBeInTheDocument()
  })
})
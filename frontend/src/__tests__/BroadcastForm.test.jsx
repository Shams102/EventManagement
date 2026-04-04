import React from 'react'
import { render, screen } from '@testing-library/react'
import BroadcastForm from '../ui/BroadcastForm'

describe('BroadcastForm', () => {
  it('renders correctly and has disabled send button initially', () => {
    render(<BroadcastForm />)
    expect(screen.getByRole('button', { name: /send/i })).toBeDisabled()
  })
})
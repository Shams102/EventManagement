import React from 'react'

export default function TimeSelect({
  value,
  onChange,
  placeholder = 'Select time',
  className = 'form-input',
  id,
  name,
  disabled,
  required,
}) {
  return (
    <input
      type="time"
      id={id}
      name={name}
      className={className}
      value={value || ''}
      onChange={(e) => onChange?.(e.target.value)}
      disabled={disabled}
      required={required}
      placeholder={placeholder}
      style={{ colorScheme: 'dark' }}
    />
  )
}

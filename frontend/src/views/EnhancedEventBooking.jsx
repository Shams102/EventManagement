import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import api from '../lib/api'

const FIELD_OPTIONS = [
  { key: 'full_name', label: 'Full Name' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Phone Number' },
  { key: 'student_id', label: 'Student ID' },
  { key: 'department', label: 'Department' },
  { key: 'year', label: 'Year' },
  { key: 'tshirt_size', label: 'T-shirt Size' },
  { key: 'dietary_pref', label: 'Dietary Preference' },
  { key: 'comments', label: 'Comments' }
]

const EnhancedEventBooking = () => {
  const { hasRole, clubId } = useAuth()
  const navigate = useNavigate()
  
  // Event Details
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [club, setClub] = useState(clubId || '')
  const [selected, setSelected] = useState(['full_name', 'email'])
  
  // Room Booking
  const [buildings, setBuildings] = useState([])
  const [selectedBuilding, setSelectedBuilding] = useState('')
  const [floors, setFloors] = useState([])
  const [selectedFloor, setSelectedFloor] = useState('')
  const [rooms, setRooms] = useState([])
  const [selectedRoom, setSelectedRoom] = useState('')
  const [selectedDate, setSelectedDate] = useState('')
  const [availableSlots, setAvailableSlots] = useState([])
  const [selectedSlots, setSelectedSlots] = useState([])
  const [roomSchedule, setRoomSchedule] = useState([])
  const [showSchedule, setShowSchedule] = useState(false)
  const [needsRoom, setNeedsRoom] = useState(false)
  
  // UI State
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  // Fixed time slots
  const timeSlots = [
    '9:00-9:50', '9:50-10:40', '10:40-11:30', '11:30-12:20',
    '12:20-13:10', '13:10-14:00', '14:00-14:50', '14:50-15:40',
    '15:40-16:30', '16:30-17:20', '17:20-18:10'
  ]

  const allowed = hasRole('ADMIN') || hasRole('FACULTY') || hasRole('CLUB_ASSOCIATE')

  // Fetch buildings on mount
  useEffect(() => {
    if (needsRoom) {
      fetchBuildings()
    }
  }, [needsRoom])

  // Fetch floors when building selected
  useEffect(() => {
    if (selectedBuilding) {
      fetchFloors(selectedBuilding)
    }
  }, [selectedBuilding])

  // Fetch rooms when floor selected
  useEffect(() => {
    if (selectedFloor) {
      fetchRooms(selectedFloor)
    }
  }, [selectedFloor])

  // Check availability when room and date selected
  useEffect(() => {
    if (selectedRoom && selectedDate) {
      checkRoomAvailability()
      fetchRoomSchedule()
    }
  }, [selectedRoom, selectedDate])

  const fetchBuildings = async () => {
    try {
      const response = await api.get('/api/room-management/buildings')
      setBuildings((response.data || []).filter(b => b.id && b.name))
    } catch (error) {
      console.error('Failed to fetch buildings:', error)
      setError('Failed to load buildings. Please try again.')
    }
  }

  const fetchFloors = async (buildingId) => {
    try {
      const response = await api.get(`/api/room-management/buildings/${buildingId}/floors`)
      setFloors(response.data)
      setSelectedFloor('')
      setRooms([])
      setSelectedRoom('')
      setSelectedSlots([])
    } catch (error) {
      console.error('Failed to fetch floors:', error)
      setError('Failed to load floors. Please try again.')
    }
  }

  const fetchRooms = async (floorId) => {
    try {
      const response = await api.get(`/api/room-management/floors/${floorId}/rooms`)
      setRooms(response.data)
      setSelectedRoom('')
      setSelectedSlots([])
    } catch (error) {
      console.error('Failed to fetch rooms:', error)
      setError('Failed to load rooms. Please try again.')
    }
  }

  const checkRoomAvailability = async () => {
    try {
      setLoading(true)
      const response = await api.get(
        `/api/room-management/rooms/${selectedRoom}/availability?date=${selectedDate}`
      )
      setAvailableSlots(response.data.availableSlots || [])
      setSelectedSlots([])
    } catch (error) {
      console.error('Failed to check availability:', error)
      setError('Failed to check room availability.')
    } finally {
      setLoading(false)
    }
  }

  const fetchRoomSchedule = async () => {
    try {
      const response = await api.get(
        `/api/room-management/rooms/${selectedRoom}/schedule?date=${selectedDate}`
      )
      setRoomSchedule(response.data || [])
    } catch (error) {
      console.error('Failed to fetch schedule:', error)
    }
  }

  const handleSlotSelection = (slot) => {
    if (!availableSlots.includes(slot)) return

    setSelectedSlots(prev => {
      if (prev.includes(slot)) {
        return prev.filter(s => s !== slot)
      } else {
        const newSelection = [...prev, slot].sort()
        if (areSlotsConsecutive(newSelection)) {
          return newSelection
        } else {
          setError('Please select consecutive time slots only.')
          return prev
        }
      }
    })
    setError('')
  }

  const areSlotsConsecutive = (slots) => {
    if (slots.length <= 1) return true
    
    const sortedSlots = [...slots].sort()
    for (let i = 1; i < sortedSlots.length; i++) {
      const currentIndex = timeSlots.indexOf(sortedSlots[i - 1])
      const nextIndex = timeSlots.indexOf(sortedSlots[i])
      if (nextIndex !== currentIndex + 1) {
        return false
      }
    }
    return true
  }

  const toggleField = (key) => {
    setSelected((prev) => prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key])
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    if (!allowed) { 
      setError('Not authorized') 
      return 
    }
    
    if (needsRoom && selectedSlots.length === 0) {
      setError('Please select at least one time slot for room booking')
      return
    }

    if (needsRoom && selectedSlots.length > 3) {
      setError('Maximum 3 consecutive slots allowed')
      return
    }

    setError('')
    setMessage('')
    
    try {
      setLoading(true)
      const registrationSchema = JSON.stringify(selected)
      
      let eventData = {
        title,
        description,
        registrationSchema,
        clubId: club || null
      }

      if (needsRoom && selectedRoom && selectedDate && selectedSlots.length > 0) {
        // Add room booking details
        const startTime = selectedSlots[0].split('-')[0]
        const endTime = selectedSlots[selectedSlots.length - 1].split('-')[1]
        
        eventData.start = `${selectedDate} ${startTime}:00`
        eventData.end = `${selectedDate} ${endTime}:00`
        eventData.location = `Room ${rooms.find(r => r.id.toString() === selectedRoom)?.roomNumber}`
        eventData.roomId = selectedRoom
      } else {
        // Manual time entry if no room booking
        const startValue = start ? `${start}:00` : null
        const endValue = end ? `${end}:00` : null
        if (startValue) eventData.start = startValue
        if (endValue) eventData.end = endValue
        if (loc) eventData.location = loc
      }

      const res = await api.post('/api/events', eventData)
      setMessage('Event created successfully!')
      
      // Reset form
      setTitle('')
      setDescription('')
      setLoc('')
      setStart('')
      setEnd('')
      setSelectedRoom('')
      setSelectedSlots([])
      
      setTimeout(() => navigate('/events'), 2000)
    } catch (err) {
      setError('Failed to create event: ' + (err.response?.data || err.message))
    } finally {
      setLoading(false)
    }
  }

  const getSelectedRoomDetails = () => {
    return rooms.find(room => room.id.toString() === selectedRoom)
  }

  const getSlotSelectionInfo = () => {
    if (selectedSlots.length === 0) return null
    const startTime = selectedSlots[0].split('-')[0]
    const endTime = selectedSlots[selectedSlots.length - 1].split('-')[1]
    return `${startTime} - ${endTime} (${selectedSlots.length} slot${selectedSlots.length > 1 ? 's' : ''})`
  }

  if (!allowed) {
    return (
      <div className="max-w-2xl mx-auto p-6">
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          You are not authorized to create events.
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h2 className="text-2xl font-bold mb-6">Create New Event</h2>
      
      {/* Messages */}
      {message && (
        <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4">
          {message}
        </div>
      )}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-6">
        {/* Basic Event Details */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold mb-4">Event Details</h3>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2">Event Title *</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                className="w-full p-2 border rounded"
                placeholder="Enter event title"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-2">Description</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={4}
                className="w-full p-2 border rounded"
                placeholder="Describe your event"
              />
            </div>

            {hasRole('ADMIN') && (
              <div>
                <label className="block text-sm font-medium mb-2">Club</label>
                <input
                  type="number"
                  value={club}
                  onChange={(e) => setClub(e.target.value)}
                  className="w-full p-2 border rounded"
                  placeholder="Club ID (Admin only)"
                />
              </div>
            )}
          </div>
        </div>

        {/* Room Booking Toggle */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold mb-4">Room Booking</h3>
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={needsRoom}
              onChange={(e) => setNeedsRoom(e.target.checked)}
              className="mr-2"
            />
            <span>Book a room for this event</span>
          </label>
        </div>

        {/* Room Selection (if needed) */}
        {needsRoom && (
          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-lg font-semibold mb-4">Select Room & Time</h3>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
              <div>
                <label className="block text-sm font-medium mb-2">Building</label>
                <select
                  value={selectedBuilding}
                  onChange={(e) => setSelectedBuilding(e.target.value)}
                  className="w-full p-2 border rounded"
                >
                  <option value="">Select Building</option>
                  {buildings.map(building => (
                    <option key={building.id} value={building.id}>
                      {building.name}
                    </option>
                  ))}
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-2">Floor</label>
                <select
                  value={selectedFloor}
                  onChange={(e) => setSelectedFloor(e.target.value)}
                  className="w-full p-2 border rounded"
                  disabled={!selectedBuilding}
                >
                  <option value="">Select Floor</option>
                  {floors.map(floor => (
                    <option key={floor.id} value={floor.id}>
                      {floor.name}
                    </option>
                  ))}
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-2">Room</label>
                <select
                  value={selectedRoom}
                  onChange={(e) => setSelectedRoom(e.target.value)}
                  className="w-full p-2 border rounded"
                  disabled={!selectedFloor}
                >
                  <option value="">Select Room</option>
                  {rooms.map(room => (
                    <option key={room.id} value={room.id}>
                      {room.roomNumber} - {room.name} (Capacity: {room.capacity})
                    </option>
                  ))}
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-2">Date</label>
                <input
                  type="date"
                  value={selectedDate}
                  onChange={(e) => setSelectedDate(e.target.value)}
                  min={new Date().toISOString().split('T')[0]}
                  className="w-full p-2 border rounded"
                />
              </div>
            </div>
            
            {/* Room Details */}
            {selectedRoom && getSelectedRoomDetails() && (
              <div className="bg-gray-50 p-4 rounded mb-4">
                <h4 className="font-medium mb-2">Room Details</h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                  <div><strong>Room:</strong> {getSelectedRoomDetails().roomNumber}</div>
                  <div><strong>Type:</strong> {getSelectedRoomDetails().type}</div>
                  <div><strong>Capacity:</strong> {getSelectedRoomDetails().capacity}</div>
                  <div><strong>Amenities:</strong> {getSelectedRoomDetails().amenities}</div>
                </div>
              </div>
            )}
            
            {selectedRoom && selectedDate && (
              <div>
                <div className="flex justify-between items-center mb-4">
                  <h3 className="font-semibold">Available Time Slots</h3>
                  <div className="text-sm text-gray-600">
                    Select up to 3 consecutive slots
                  </div>
                  <button
                    type="button"
                    onClick={() => setShowSchedule(!showSchedule)}
                    className="text-blue-600 hover:text-blue-800 text-sm"
                  >
                    {showSchedule ? 'Hide' : 'Show'} Room Schedule
                  </button>
                </div>
                
                {loading ? (
                  <div className="text-center py-4">Checking availability...</div>
                ) : (
                  <>
                    <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-2 mb-4">
                      {timeSlots.map(slot => {
                        const isAvailable = availableSlots.includes(slot)
                        const isSelected = selectedSlots.includes(slot)
                        return (
                          <button
                            key={slot}
                            type="button"
                            onClick={() => handleSlotSelection(slot)}
                            disabled={!isAvailable}
                            className={`p-3 rounded text-sm transition-colors ${
                              isSelected
                                ? 'bg-blue-500 text-white border-blue-600'
                                : isAvailable
                                ? 'bg-green-100 hover:bg-green-200 text-green-800 border border-green-300'
                                : 'bg-red-100 text-red-400 cursor-not-allowed border border-red-200'
                            }`}
                          >
                            <div>{slot}</div>
                            {!isAvailable && <div className="text-xs">Occupied</div>}
                            {isSelected && <div className="text-xs">Selected</div>}
                          </button>
                        )
                      })}
                    </div>

                    {/* Selection Summary */}
                    {selectedSlots.length > 0 && (
                      <div className="bg-blue-50 p-4 rounded mb-4">
                        <h4 className="font-medium mb-2">Selected Time</h4>
                        <div className="text-lg font-semibold text-blue-700">
                          {getSlotSelectionInfo()}
                        </div>
                      </div>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        )}

        {/* Manual Time Entry (if no room booking) */}
        {!needsRoom && (
          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-lg font-semibold mb-4">Time & Location</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium mb-2">Start Time</label>
                <input
                  type="datetime-local"
                  value={start}
                  onChange={(e) => setStart(e.target.value)}
                  className="w-full p-2 border rounded"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">End Time</label>
                <input
                  type="datetime-local"
                  value={end}
                  onChange={(e) => setEnd(e.target.value)}
                  className="w-full p-2 border rounded"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-2">Location</label>
                <input
                  type="text"
                  value={loc}
                  onChange={(e) => setLoc(e.target.value)}
                  className="w-full p-2 border rounded"
                  placeholder="Event location"
                />
              </div>
            </div>
          </div>
        )}

        {/* Registration Fields */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold mb-4">Registration Fields</h3>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
            {FIELD_OPTIONS.map(({ key, label }) => (
              <label key={key} className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  checked={selected.includes(key)}
                  onChange={() => toggleField(key)}
                  className="rounded"
                />
                <span className="text-sm">{label}</span>
              </label>
            ))}
          </div>
        </div>

        {/* Submit Button */}
        <div className="flex justify-end">
          <button
            type="submit"
            disabled={loading || (needsRoom && selectedSlots.length === 0)}
            className="bg-blue-500 text-white px-6 py-2 rounded hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Creating...' : 'Create Event'}
          </button>
        </div>
      </form>

      {/* Room Schedule View */}
      {showSchedule && selectedRoom && (
        <RoomScheduleView 
          roomId={selectedRoom} 
          date={selectedDate}
          schedule={roomSchedule}
        />
      )}
    </div>
  )
}

// Room Schedule View Component
const RoomScheduleView = ({ roomId, date, schedule }) => {
  const timeSlots = [
    '9:00-9:50', '9:50-10:40', '10:40-11:30', '11:30-12:20',
    '12:20-13:10', '13:10-14:00', '14:00-14:50', '14:50-15:40',
    '15:40-16:30', '16:30-17:20', '17:20-18:10'
  ]

  const getScheduleItem = (timeSlot) => {
    const [startTime] = timeSlot.split('-')
    const item = schedule.find(s => s.startTime === startTime + ':00')
    
    if (!item) return null
    
    return (
      <div className={`p-2 rounded text-xs ${
        item.type === 'FIXED_CLASS' 
          ? 'bg-blue-100 border-blue-300' 
          : 'bg-green-100 border-green-300'
      } border`}>
        <div className="font-semibold">{item.title}</div>
        <div className="text-gray-600">{item.subtitle}</div>
        <div className="text-gray-500">{item.facultyName}</div>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden mt-6">
      <div className="p-4 border-b bg-gray-50">
        <h3 className="text-lg font-semibold">
          Room Schedule for {new Date(date).toLocaleDateString()}
        </h3>
      </div>
      
      <div className="p-4">
        <div className="grid grid-cols-1 gap-2">
          {timeSlots.map(timeSlot => (
            <div key={timeSlot} className="flex border-b pb-2">
              <div className="w-24 font-medium text-sm text-gray-600">
                {timeSlot}
              </div>
              <div className="flex-1 min-h-[60px]">
                {getScheduleItem(timeSlot) || (
                  <div className="text-gray-400 text-sm italic">Available</div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default EnhancedEventBooking

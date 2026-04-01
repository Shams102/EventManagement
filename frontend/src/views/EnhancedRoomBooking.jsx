import React, { useState, useEffect } from 'react'
import api from '../lib/api'

const EnhancedRoomBooking = () => {
  // Force re-render with timestamp
  const [forceUpdate] = useState(Date.now())
  
  const [buildings, setBuildings] = useState([])
  const [selectedBuilding, setSelectedBuilding] = useState('')
  const [floors, setFloors] = useState([])
  const [selectedFloor, setSelectedFloor] = useState('')
  const [rooms, setRooms] = useState([])
  const [selectedRoom, setSelectedRoom] = useState('')
  const [selectedDate, setSelectedDate] = useState('')
  const [availableSlots, setAvailableSlots] = useState([])
  const [selectedSlots, setSelectedSlots] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [bookingType, setBookingType] = useState('meeting')



  // Fixed time slots - matching backend TimeSlot enum
  const timeSlots = [
    '09:00-09:50', '09:50-10:40', '10:40-11:30', '11:30-12:20',
    '12:20-13:10', '13:10-14:00', '14:00-14:50', '14:50-15:40',
    '15:40-16:30', '16:30-17:20', '17:20-18:10'
  ]

  useEffect(() => {
    fetchBuildings()
  }, [])

  useEffect(() => {
    if (selectedBuilding) {
      fetchFloors(selectedBuilding)
    }
  }, [selectedBuilding])

  useEffect(() => {
    if (selectedFloor) {
      fetchRooms(selectedFloor)
    }
  }, [selectedFloor])

  useEffect(() => {
    if (selectedRoom && selectedDate) {
      checkRoomAvailability()
    }
  }, [selectedRoom, selectedDate])

  const fetchBuildings = async () => {
    try {
      const response = await api.get('/api/room-management/buildings')
      setBuildings(response.data || [])
    } catch (error) {
      console.error('Failed to fetch buildings:', error)
      setError('Failed to load buildings')
    }
  }

  const fetchFloors = async (buildingId) => {
    try {
      const response = await api.get(`/api/room-management/buildings/${buildingId}/floors`)
      setFloors(response.data || [])
      setSelectedFloor('')
      setRooms([])
      setSelectedRoom('')
      setSelectedSlots([])
    } catch (error) {
      console.error('Failed to fetch floors:', error)
      setError('Failed to load floors')
    }
  }

  const fetchRooms = async (floorId) => {
    try {
      const response = await api.get(`/api/room-management/floors/${floorId}/rooms`)
      setRooms(response.data || [])
      setSelectedRoom('')
      setSelectedSlots([])
    } catch (error) {
      console.error('Failed to fetch rooms:', error)
      setError('Failed to load rooms')
    }
  }

  const checkRoomAvailability = async () => {
    try {
      setLoading(true)
      const response = await api.get(
        `/api/room-management/rooms/${selectedRoom}/availability?date=${selectedDate}`
      )
      setAvailableSlots(response.data?.availableSlots || [])
      setSelectedSlots([])
    } catch (error) {
      console.error('Failed to check availability:', error)
      setError('Failed to check room availability')
      setAvailableSlots([])
    } finally {
      setLoading(false)
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
          setError('Please select consecutive time slots only')
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

  const handleBooking = async () => {
    if (selectedSlots.length === 0) {
      setError('Please select at least one time slot')
      return
    }

    try {
      setLoading(true)
      const startTime = selectedSlots[0].split('-')[0]
      const endTime = selectedSlots[selectedSlots.length - 1].split('-')[1]
      
      const bookingData = {
        roomId: selectedRoom,
        purpose: `${bookingType} - ${selectedSlots.length} slot(s)`,
        meetingStart: `${selectedDate} ${startTime}:00`,
        meetingEnd: `${selectedDate} ${endTime}:00`
      }

      const response = await api.post('/api/room-requests/meeting', bookingData)
      setMessage('Room booking request submitted successfully!')
      setSelectedSlots([])
      
      // Refresh availability
      await checkRoomAvailability()
    } catch (error) {
      console.error('Failed to submit booking:', error)
      setError('Failed to submit booking request')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-lg p-6">
        {/* Cache-Busting Header */}
        <div className="bg-yellow-100 border-2 border-yellow-300 p-4 rounded-lg mb-6">
          <h1 className="text-2xl font-bold text-yellow-800">🏛️ ENHANCED ROOM BOOKING - NEW VERSION</h1>
          <p className="text-yellow-700">If you see this yellow box, the cache clearing worked!</p>
          <p className="text-sm text-yellow-600">Loaded at: {new Date().toLocaleString()}</p>
        </div>
        
        <h1 className="text-3xl font-bold text-gray-900 mb-8">🏛️ Enhanced Room Booking</h1>
        
        {/* Messages */}
        {message && (
          <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4">
            ✅ {message}
          </div>
        )}
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            ❌ {error}
          </div>
        )}

        {/* Step 1: Building Selection */}
        <div className="mb-8">
          <h2 className="text-xl font-semibold mb-4 text-gray-800">📍 Step 1: Select Building</h2>
          <select
            value={selectedBuilding}
            onChange={(e) => setSelectedBuilding(e.target.value)}
            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">Choose a building...</option>
            {buildings.map(building => (
              <option key={building.id} value={building.id}>
                🏢 {building.name}
              </option>
            ))}
          </select>
        </div>

        {/* Step 2: Floor Selection */}
        {selectedBuilding && (
          <div className="mb-8">
            <h2 className="text-xl font-semibold mb-4 text-gray-800">📍 Step 2: Select Floor</h2>
            <select
              value={selectedFloor}
              onChange={(e) => setSelectedFloor(e.target.value)}
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">Choose a floor...</option>
              {floors.map(floor => (
                <option key={floor.id} value={floor.id}>
                  📍 Floor {floor.floorNumber} - {floor.name}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Step 3: Room Selection */}
        {selectedFloor && (
          <div className="mb-8">
            <h2 className="text-xl font-semibold mb-4 text-gray-800">📍 Step 3: Select Room</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {rooms.map(room => (
                <div
                  key={room.id}
                  onClick={() => setSelectedRoom(room.id.toString())}
                  className={`p-4 border-2 rounded-lg cursor-pointer transition-all ${
                    selectedRoom === room.id.toString()
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  <div className="font-semibold text-lg">🚪 {room.roomNumber}</div>
                  <div className="text-sm text-gray-600">{room.name}</div>
                  <div className="text-sm text-gray-500">Type: {room.type}</div>
                  <div className="text-sm text-gray-500">Capacity: {room.capacity}</div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Step 4: Date Selection */}
        {selectedRoom && (
          <div className="mb-8">
            <h2 className="text-xl font-semibold mb-4 text-gray-800">📅 Step 4: Select Date</h2>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              min={new Date().toISOString().split('T')[0]}
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
        )}

        {/* Step 5: Time Slot Selection */}
        {selectedRoom && selectedDate && (
          <div className="mb-8">
            <h2 className="text-xl font-semibold mb-4 text-gray-800">⏰ Step 5: Select Time Slots</h2>
            
            {/* Booking Type */}
            <div className="mb-6">
              <h3 className="text-lg font-medium mb-3">Booking Type:</h3>
              <div className="flex space-x-6">
                <label className="flex items-center cursor-pointer">
                  <input
                    type="radio"
                    value="meeting"
                    checked={bookingType === 'meeting'}
                    onChange={(e) => setBookingType(e.target.value)}
                    className="mr-2"
                  />
                  <span>🤝 Meeting</span>
                </label>
                <label className="flex items-center cursor-pointer">
                  <input
                    type="radio"
                    value="event"
                    checked={bookingType === 'event'}
                    onChange={(e) => setBookingType(e.target.value)}
                    className="mr-2"
                  />
                  <span>🎉 Event</span>
                </label>
              </div>
            </div>

            {loading ? (
              <div className="text-center py-8">
                <div className="text-lg">⏳ Checking availability...</div>
              </div>
            ) : (
              <>

                {/* Time Slot Grid */}
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 mb-6">
                  {timeSlots.map(slot => {
                    const isAvailable = availableSlots.includes(slot)
                    const isSelected = selectedSlots.includes(slot)
                    return (
                      <button
                        key={slot}
                        onClick={() => handleSlotSelection(slot)}
                        disabled={!isAvailable}
                        className={`p-4 rounded-lg text-sm font-medium transition-all transform hover:scale-105 ${
                          isSelected
                            ? 'bg-blue-500 text-white border-2 border-blue-600 shadow-lg'
                            : isAvailable
                            ? 'bg-green-100 hover:bg-green-200 text-green-800 border-2 border-green-300 cursor-pointer'
                            : 'bg-red-100 text-red-400 border-2 border-red-200 cursor-not-allowed opacity-60'
                        }`}
                      >
                        <div className="font-bold">🕐 {slot}</div>
                        {!isAvailable && <div className="text-xs mt-1">❌ Occupied</div>}
                        {isSelected && <div className="text-xs mt-1">✅ Selected</div>}
                        {isAvailable && !isSelected && <div className="text-xs mt-1">✨ Available</div>}
                      </button>
                    )
                  })}
                </div>

                {/* Selection Summary */}
                {selectedSlots.length > 0 && (
                  <div className="bg-blue-50 border-2 border-blue-200 p-4 rounded-lg mb-6">
                    <h3 className="font-semibold mb-2">📋 Selection Summary:</h3>
                    <div className="text-lg">
                      <strong>Time:</strong> {selectedSlots[0]} - {selectedSlots[selectedSlots.length - 1].split('-')[1]}
                    </div>
                    <div className="text-lg">
                      <strong>Duration:</strong> {selectedSlots.length} slot(s) ({selectedSlots.length * 50} minutes)
                    </div>
                    <div className="text-lg">
                      <strong>Type:</strong> {bookingType}
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* Submit Button */}
        {selectedRoom && selectedDate && selectedSlots.length > 0 && (
          <div className="text-center">
            <button
              onClick={handleBooking}
              disabled={loading}
              className="bg-blue-500 hover:bg-blue-600 text-white font-bold py-4 px-8 rounded-lg text-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? '⏳ Submitting...' : '🚀 Book Room'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default EnhancedRoomBooking

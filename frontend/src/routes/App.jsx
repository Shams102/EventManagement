import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from '../lib/AuthContext'
import Layout from '../ui/Layout'
import Landing from '../views/Landing'
import Login from '../views/Login'
import Dashboard from '../views/Dashboard'
import Events from '../views/Events'
import Bookings from '../views/Bookings'
import EventRegistration from '../views/EventRegistration'
import RoomBooking from '../views/RoomBooking'
import Register from '../views/Register'
import ProtectedRoute from './ProtectedRoute'
import AdminRoleRequests from '../views/AdminRoleRequests'
import CreateEvent from '../views/CreateEvent'
import EventEdit from '../views/EventEdit'
import AdminRoomApprovals from '../views/AdminRoomApprovals'
import AdminBroadcast from '../views/AdminBroadcast'
import StyleGuide from '../views/StyleGuide'
import NotificationsPage from '../views/NotificationsPage'
import ThreadChatPage from '../views/ThreadChatPage'

export default function App() {
  return (
    <AuthProvider>
      <Layout>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
          <Route path="/events" element={<Events />} />
          <Route path="/events/create" element={<ProtectedRoute roles={['FACULTY','CLUB_ASSOCIATE','ADMIN']}><CreateEvent /></ProtectedRoute>} />
          <Route path="/events/edit/:id" element={<ProtectedRoute roles={['FACULTY','CLUB_ASSOCIATE','ADMIN']}><EventEdit /></ProtectedRoute>} />
          <Route path="/bookings" element={<ProtectedRoute roles={['FACULTY','CLUB_ASSOCIATE','ADMIN']}><Bookings /></ProtectedRoute>} />
          <Route path="/register/:eventId" element={<ProtectedRoute roles={['GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN']}><EventRegistration /></ProtectedRoute>} />
          <Route path="/book-room" element={<ProtectedRoute roles={['FACULTY','CLUB_ASSOCIATE','ADMIN']}><RoomBooking /></ProtectedRoute>} />
          <Route path="/enhanced-book-room" element={<ProtectedRoute roles={['FACULTY','CLUB_ASSOCIATE','ADMIN']}><RoomBooking /></ProtectedRoute>} />
          <Route path="/admin/role-requests" element={<ProtectedRoute roles={['ADMIN']}><AdminRoleRequests /></ProtectedRoute>} />
          <Route path="/admin/room-approvals" element={<ProtectedRoute roles={['ADMIN']}><AdminRoomApprovals /></ProtectedRoute>} />
          <Route path="/admin/notifications" element={<ProtectedRoute roles={[ 'ADMIN' ]}><AdminBroadcast /></ProtectedRoute>} />
          <Route path="/notifications" element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />
          <Route path="/notifications/threads/:threadId" element={<ProtectedRoute><ThreadChatPage /></ProtectedRoute>} />
          <Route path="/style-guide" element={<StyleGuide />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Layout>
    </AuthProvider>
  )
}



import axios from 'axios'

const baseURL = import.meta.env?.VITE_API_BASE_URL || ''

const api = axios.create({
  baseURL,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Retry once on transient transport errors (e.g., HTTP/2 GOAWAY during server restart)
api.interceptors.response.use(undefined, async (error) => {
  const cfg = error?.config || {}
  const msg = String(error?.message || '')
  const isTransient = msg.includes('server_shutting_down') || msg.includes('GOAWAY') || (!error.response && error.request)
  if (isTransient && !cfg._retry) {
    cfg._retry = true
    await new Promise(r => setTimeout(r, 500))
    return api.request(cfg)
  }

  const status = error?.response?.status
  if (status === 401) {
    try {
      localStorage.removeItem('token')
    } catch {
      // ignore
    }
    if (window.location.pathname !== '/login') {
      window.location.assign('/login')
    }
  }
  return Promise.reject(error)
})


// Notifications API
export const fetchInbox = () => api.get('/api/notifications')
export const fetchMyLegacyNotifications = () => api.get('/api/notifications/mine')
export const broadcastNotification = (body) => api.post('/api/notifications/broadcast', body)
export const postEventNotification = (eventId, body) => api.post(`/api/notifications/events/${eventId}`, body)
export const fetchEventNotifications = (eventId) => api.get(`/api/notifications/events/${eventId}`)
export const markDeliveryRead = (deliveryId) => api.post(`/api/notifications/deliveries/${deliveryId}/mark-read`)
export const muteDelivery = (deliveryId, mute) => api.post(`/api/notifications/deliveries/${deliveryId}/mute`, { mute })

// Threads
export const createThread = (body) => api.post('/api/notifications/threads', body)
export const fetchThreadMessages = (threadId) => api.get(`/api/notifications/threads/${threadId}/messages`)
export const postThreadMessage = (threadId, body) => api.post(`/api/notifications/threads/${threadId}/messages`, body)

export default api



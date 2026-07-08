let socket = null
let token = ''
let reconnectTimer = null
let manualClose = false
let status = 'disconnected'

const messageListeners = new Set()
const statusListeners = new Set()

export function connectRealtime(nextToken) {
  if (!nextToken) return
  token = nextToken
  manualClose = false
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return
  }
  openSocket()
}

export function disconnectRealtime() {
  manualClose = true
  token = ''
  clearReconnect()
  if (socket) {
    socket.close()
    socket = null
  }
  setStatus('disconnected')
}

export function sendRealtime(payload) {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    return false
  }
  socket.send(JSON.stringify(payload))
  return true
}

export function getRealtimeStatus() {
  return status
}

export function onRealtimeMessage(listener) {
  messageListeners.add(listener)
  return () => messageListeners.delete(listener)
}

export function onRealtimeStatus(listener) {
  statusListeners.add(listener)
  listener(status)
  return () => statusListeners.delete(listener)
}

function openSocket() {
  clearReconnect()
  setStatus('connecting')
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const url = `${protocol}//${window.location.host}/ws?token=${encodeURIComponent(token)}`
  socket = new WebSocket(url)

  socket.onopen = () => {
    setStatus('connected')
  }

  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      messageListeners.forEach((listener) => listener(payload))
    } catch (error) {
      console.warn('Invalid websocket message', error)
    }
  }

  socket.onerror = () => {
    setStatus('error')
  }

  socket.onclose = () => {
    socket = null
    if (manualClose || !token) {
      setStatus('disconnected')
      return
    }
    setStatus('reconnecting')
    reconnectTimer = window.setTimeout(openSocket, 3000)
  }
}

function clearReconnect() {
  if (reconnectTimer) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

function setStatus(nextStatus) {
  status = nextStatus
  statusListeners.forEach((listener) => listener(status))
}

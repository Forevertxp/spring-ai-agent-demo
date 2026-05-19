import axios from 'axios'

const API_BASE = '/api/agent'

export async function analyze(sessionId, query) {
  const response = await axios.post(`${API_BASE}/analyze`, {
    query
  }, {
    headers: {
      'X-Session-Id': sessionId
    }
  })
  return response.data
}

export async function analyzeStream(sessionId, query, onChunk) {
  const response = await fetch(`${API_BASE}/analyze/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Session-Id': sessionId
    },
    body: JSON.stringify({ query })
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    const chunk = decoder.decode(value, { stream: true })
    onChunk(chunk)
  }
}

export async function followUp(sessionId, question) {
  const response = await axios.post(`${API_BASE}/follow-up`, {
    question
  }, {
    headers: {
      'X-Session-Id': sessionId
    }
  })
  return response.data
}
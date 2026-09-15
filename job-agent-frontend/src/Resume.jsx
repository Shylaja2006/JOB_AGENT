import { useState } from 'react'
import './Resume.css'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

function Resume({ onBack }) {
  const [resumeText, setResumeText] = useState('')
  const [skills, setSkills] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSave = async (e) => {
    e.preventDefault()

    setMessage('')
    setError('')
    setLoading(true)

    try {
      const token = localStorage.getItem('token')

      if (!token) {
        throw new Error('Please login again.')
      }

      const response = await fetch(`${API_URL}/api/resume`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          resumeText,
          skills,
        }),
      })

      const result = await response.text()

      if (!response.ok) {
        throw new Error(result || 'Failed to save resume')
      }

      setMessage('Resume saved successfully! 🎉')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="dashboard">

      <header className="dashboard-header">
        <div>
          <h1>💼 Job Agent</h1>
          <p>My Resume</p>
        </div>

        <button
          className="logout-button"
          onClick={onBack}
        >
          ← Back
        </button>
      </header>

      <main className="dashboard-content">

        <button
          className="back-button"
          onClick={onBack}
        >
          ← Back to Dashboard
        </button>

        <section className="welcome-card">

          <h2>📄 My Resume</h2>

          <p>
            Add your resume details and skills to get better job matches.
          </p>

          <form
            className="resume-form"
            onSubmit={handleSave}
          >

            <label>
              Resume
            </label>

            <textarea
              value={resumeText}
              onChange={(e) => setResumeText(e.target.value)}
              placeholder="Paste your resume content here..."
              rows="12"
              required
            />

            <label>
              Skills
            </label>

            <input
              type="text"
              value={skills}
              onChange={(e) => setSkills(e.target.value)}
              placeholder="Example: Java, Spring Boot, React, SQL"
              required
            />

            <button
              type="submit"
              disabled={loading}
            >
              {loading ? 'Saving...' : 'Save Resume'}
            </button>

          </form>

          {message && (
            <p className="success-message">
              {message}
            </p>
          )}

          {error && (
            <p className="error-message">
              {error}
            </p>
          )}

        </section>

      </main>
    </div>
  )
}

export default Resume
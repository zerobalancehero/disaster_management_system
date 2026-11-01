import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { useAuth } from '../features/auth/AuthContext'
import { homePath } from '../lib/transitions'
import { Btn } from '../components/ui'
import type { ApiError } from '../types'
import '../styles/Login.css'

const schema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
})

type Form = z.infer<typeof schema>

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: {
      username: 'admin@disaster.local',
      password: 'Admin@123',
    },
  })

  useEffect(() => {
    if (user) {
      navigate(homePath(user.role), { replace: true })
    }
  }, [user, navigate])

  const fillCredentials = (u: string, p: string) => {
    setValue('username', u, { shouldValidate: true })
    setValue('password', p, { shouldValidate: true })
    setError(null)
  }

  const onSubmit = async (v: Form) => {
    setError(null)
    try {
      const loggedIn = await login(v)
      navigate(homePath(loggedIn.role), { replace: true })
    } catch (e) {
      const err = e as ApiError
      setError(
        err?.message || 'Authentication failed. Please verify credentials.'
      )
    }
  }

  return (
    <div className="login-page">

      {/* Background video */}
      <video className="login-bg-video" autoPlay muted loop playsInline>
        <source src="/videos/login-bg.mp4" type="video/mp4" />
        Your browser does not support video playback.
      </video>

      {/* Cinematic dark overlay */}
      <div className="login-overlay-dark" />

      {/* Gradient overlay */}
      <div className="login-overlay-gradient" />

      {/* Login container */}
      <div className="login-container">

        <div className="login-card">

          {/* Header */}
          <div className="login-header">

            <div className="login-badge">
              <span className="login-badge-dot" />
              Emergency Operations Platform
            </div>

            <h1 className="login-title">Welcome Back</h1>

            <p className="login-subtitle">
              Sign in to access the Disaster Response Command Center
            </p>
          </div>

          {/* Quick test credentials */}
          <div className="login-quick">

            <p className="login-quick-title">Quick Test Credentials</p>

            <div className="login-quick-grid">

              <button
                type="button"
                onClick={() =>
                  fillCredentials('admin@disaster.local', 'Admin@123')
                }
                className="login-quick-btn is-admin"
              >
                <span className="login-quick-role">Admin</span>
                <span className="login-quick-desc">Full Dispatch</span>
              </button>

              <button
                type="button"
                onClick={() =>
                  fillCredentials('responder@disaster.local', 'Responder@123')
                }
                className="login-quick-btn is-responder"
              >
                <span className="login-quick-role">Responder</span>
                <span className="login-quick-desc">Field Squad</span>
              </button>

              <button
                type="button"
                onClick={() =>
                  fillCredentials('user@disaster.local', 'User@123')
                }
                className="login-quick-btn is-citizen"
              >
                <span className="login-quick-role">Citizen</span>
                <span className="login-quick-desc">Report Alerts</span>
              </button>

            </div>
          </div>

          {/* Login form */}
          <form className="login-form" onSubmit={handleSubmit(onSubmit)}>

            <div className="login-field">
              <label className="login-label">Email / Username</label>
              <input
                className="login-input"
                placeholder="Enter your username"
                autoComplete="username"
                {...register('username')}
              />
              {errors.username && (
                <p className="login-field-error">{errors.username.message}</p>
              )}
            </div>

            <div className="login-field">
              <label className="login-label">Password</label>
              <input
                type="password"
                className="login-input"
                placeholder="Enter your password"
                autoComplete="current-password"
                {...register('password')}
              />
              {errors.password && (
                <p className="login-field-error">{errors.password.message}</p>
              )}
            </div>

            {error && (
              <div role="alert" className="login-alert">
                ⚠️ {error}
              </div>
            )}

            <Btn
              type="submit"
              disabled={isSubmitting}
              className="login-submit"
            >
              {isSubmitting ? 'Authenticating...' : 'Sign in to Console →'}
            </Btn>

          </form>

          {/* Footer */}
          <div className="login-footer">
            <p>Secure Disaster Management & Emergency Response System</p>
          </div>

        </div>
      </div>
    </div>
  )
}
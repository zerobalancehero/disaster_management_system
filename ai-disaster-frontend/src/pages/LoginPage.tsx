import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { useAuth } from '../features/auth/AuthContext'
import { homePath } from '../lib/transitions'
import { Btn } from '../components/ui'
import type { ApiError } from '../types'

const schema = z.object({ username: z.string().min(1, 'Username is required'), password: z.string().min(1, 'Password is required') })
type Form = z.infer<typeof schema>

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, setValue, formState: { errors, isSubmitting } } = useForm<Form>({
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
      setError(err?.message || 'Authentication failed. Please verify credentials.')
    }
  }

  return (
    <div className="mx-auto mt-16 max-w-md space-y-6 rounded-2xl border border-slate-800 bg-slate-900/90 p-8 shadow-2xl backdrop-blur-md">
      <div className="text-center space-y-1">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium bg-red-500/10 text-red-400 border border-red-500/20 mb-2">
          🚨 Emergency Operations Platform
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-white">Sign In</h1>
        <p className="text-xs text-slate-400">Authenticate to access incident dispatch and reporting</p>
      </div>

      <div className="space-y-2">
        <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Quick Test Credentials:</p>
        <div className="grid grid-cols-3 gap-2">
          <button
            type="button"
            onClick={() => fillCredentials('admin@disaster.local', 'Admin@123')}
            className="flex flex-col items-center justify-center rounded-lg border border-slate-700 bg-slate-800/80 p-2 text-xs hover:border-red-500/50 hover:bg-slate-700/80 transition-all cursor-pointer"
          >
            <span className="font-semibold text-red-400">Admin</span>
            <span className="text-[10px] text-slate-400">Full Dispatch</span>
          </button>
          <button
            type="button"
            onClick={() => fillCredentials('responder@disaster.local', 'Responder@123')}
            className="flex flex-col items-center justify-center rounded-lg border border-slate-700 bg-slate-800/80 p-2 text-xs hover:border-blue-500/50 hover:bg-slate-700/80 transition-all cursor-pointer"
          >
            <span className="font-semibold text-blue-400">Responder</span>
            <span className="text-[10px] text-slate-400">Field Squad</span>
          </button>
          <button
            type="button"
            onClick={() => fillCredentials('user@disaster.local', 'User@123')}
            className="flex flex-col items-center justify-center rounded-lg border border-slate-700 bg-slate-800/80 p-2 text-xs hover:border-emerald-500/50 hover:bg-slate-700/80 transition-all cursor-pointer"
          >
            <span className="font-semibold text-emerald-400">Citizen</span>
            <span className="text-[10px] text-slate-400">Report Alerts</span>
          </button>
        </div>
      </div>

      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
        <div>
          <label className="block text-xs font-medium text-slate-300 mb-1">Email / Username</label>
          <input
            className="w-full rounded-lg border border-slate-700 bg-slate-800/70 p-2.5 text-sm text-white focus:border-red-500 focus:outline-none transition-colors"
            placeholder="e.g. admin@disaster.local"
            {...register('username')}
          />
          {errors.username && <p className="mt-1 text-xs text-red-400">{errors.username.message}</p>}
        </div>

        <div>
          <label className="block text-xs font-medium text-slate-300 mb-1">Password</label>
          <input
            type="password"
            className="w-full rounded-lg border border-slate-700 bg-slate-800/70 p-2.5 text-sm text-white focus:border-red-500 focus:outline-none transition-colors"
            placeholder="••••••••"
            {...register('password')}
          />
          {errors.password && <p className="mt-1 text-xs text-red-400">{errors.password.message}</p>}
        </div>

        {error && (
          <div role="alert" className="rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-400">
            ⚠️ {error}
          </div>
        )}

        <Btn type="submit" disabled={isSubmitting} className="w-full py-2.5 font-semibold bg-red-600 hover:bg-red-500 transition-colors shadow-lg shadow-red-900/30">
          {isSubmitting ? 'Authenticating...' : 'Sign in to Console'}
        </Btn>
      </form>
    </div>
  )
}

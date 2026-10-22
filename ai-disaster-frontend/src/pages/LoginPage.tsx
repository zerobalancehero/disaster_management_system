import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Navigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../features/auth/AuthContext'
import { homePath } from '../lib/transitions'
import { Btn } from '../components/ui'
import { config } from '../lib/config'
import type { ApiError } from '../types'

const schema = z.object({ username: z.string().min(1, 'Username is required'), password: z.string().min(1, 'Password is required') })
type Form = z.infer<typeof schema>

export default function LoginPage() {
  const { user, login } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<Form>({ resolver: zodResolver(schema) })
  if (user) return <Navigate to={homePath(user.role)} replace />
  return (
    <div className="mx-auto mt-20 max-w-sm space-y-4 rounded-lg border border-slate-800 bg-slate-900 p-6">
      <h1 className="text-xl font-semibold">Sign in</h1>
      {config.useMocks && <p className="text-xs text-amber-400">DEMO MODE: user/user123 · responder/resp123 · admin/admin123</p>}
      <form className="space-y-3" onSubmit={handleSubmit(async (v) => { setError(null); try { await login(v) } catch (e) { setError((e as ApiError).message) } })}>
        <label className="block text-sm">Username
          <input className="mt-1 w-full rounded bg-slate-800 p-2" {...register('username')} />
        </label>
        {errors.username && <p className="text-sm text-red-400">{errors.username.message}</p>}
        <label className="block text-sm">Password
          <input type="password" className="mt-1 w-full rounded bg-slate-800 p-2" {...register('password')} />
        </label>
        {errors.password && <p className="text-sm text-red-400">{errors.password.message}</p>}
        {error && <p role="alert" className="text-sm text-red-400">{error}</p>}
        <Btn type="submit" disabled={isSubmitting}>Sign in</Btn>
      </form>
    </div>
  )
}

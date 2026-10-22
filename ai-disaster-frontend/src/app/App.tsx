import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router-dom'
import { AuthProvider } from '../features/auth/AuthContext'
import { router } from './router'

const qc = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: 15_000 } } })

export default function App() {
  return (
    <QueryClientProvider client={qc}>
      <AuthProvider><RouterProvider router={router} /></AuthProvider>
    </QueryClientProvider>
  )
}

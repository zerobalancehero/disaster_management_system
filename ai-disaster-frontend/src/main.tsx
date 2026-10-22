import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { config } from './lib/config'

const root = createRoot(document.getElementById('root')!)

async function boot() {
  if (config.useMocks) {
    const { worker } = await import('./mocks/browser')
    await worker.start({ onUnhandledRequest: 'error' })
  }
  const { default: App } = await import('./app/App')
  root.render(<StrictMode><App /></StrictMode>)
}

boot().catch((e) =>
  root.render(<p role="alert" className="p-6 text-red-400">Startup failed: {e instanceof Error ? e.message : String(e)}</p>))

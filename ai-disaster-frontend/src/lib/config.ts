export const config = {
  apiBaseUrl: (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api/v1',
  useMocks: import.meta.env.VITE_USE_MOCKS === 'true',
}
if (config.useMocks && import.meta.env.PROD) {
  throw new Error('VITE_USE_MOCKS=true is not allowed in production builds.')
}

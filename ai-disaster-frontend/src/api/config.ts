export const config = {
  apiBaseUrl: (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080/api/v1',
  useMocks: import.meta.env.VITE_USE_MOCKS === 'true',
}

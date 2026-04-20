type RuntimeEnv = {
  apiBaseUrl: string
}

const defaultApiBaseUrl = 'http://localhost:8080'
const apiBaseUrl = (
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? defaultApiBaseUrl
).replace(/\/+$/, '')

export const env: RuntimeEnv = {
  apiBaseUrl,
}

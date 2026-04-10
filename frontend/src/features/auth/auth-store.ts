import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import type { AuthTokenResponse, AuthUser } from './types'

type AuthState = {
  initialized: boolean
  accessToken: string | null
  refreshToken: string | null
  user: AuthUser | null
  setSession: (payload: AuthTokenResponse) => void
  setUser: (user: AuthUser) => void
  clearSession: () => void
  setInitialized: (value: boolean) => void
}

const initialState = {
  initialized: false,
  accessToken: null,
  refreshToken: null,
  user: null,
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      ...initialState,
      setSession: (payload) =>
        set({
          accessToken: payload.accessToken,
          refreshToken: payload.refreshToken,
          user: payload.user,
        }),
      setUser: (user) => set({ user }),
      clearSession: () =>
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
        }),
      setInitialized: (isInitialized) => set({ initialized: isInitialized }),
    }),
    {
      name: 'EMR-auth-session',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    }
  )
)

export const authSelectors = {
  isAuthenticated: (state: AuthState) => Boolean(state.accessToken && state.user),
}

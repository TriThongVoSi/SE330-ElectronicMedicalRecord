import { createContext } from 'react'

export type AppLanguage = 'en' | 'vi'
export type TranslationDictionary = {
  [key: string]: string | TranslationDictionary
}
export type TranslationParams = Record<string, string | number>

export type I18nContextValue = {
  language: AppLanguage
  setLanguage: (language: AppLanguage) => void
  t: (key: string, fallback: string, params?: TranslationParams) => string
}

export const I18nContext = createContext<I18nContextValue | null>(null)

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import {
  type AppLanguage,
  I18nContext,
  type TranslationDictionary,
  type TranslationParams,
} from './i18n-context'

const I18N_LANGUAGE_STORAGE_KEY = 'se330.language'

const getDictionaryValue = (dictionary: TranslationDictionary, key: string): string | null => {
  let current: string | TranslationDictionary | undefined = dictionary

  for (const segment of key.split('.')) {
    if (typeof current !== 'object' || current === null) {
      return null
    }

    current = current[segment]
  }

  return typeof current === 'string' ? current : null
}

const applyTemplateParams = (template: string, params?: TranslationParams): string => {
  if (!params) {
    return template
  }

  return template.replace(/\{(\w+)\}/g, (_, key: string) => {
    const value = params[key]
    return value === undefined ? '' : String(value)
  })
}

type I18nProviderProps = {
  children: React.ReactNode
}

export const I18nProvider: React.FC<I18nProviderProps> = ({ children }) => {
  const [language, setLanguage] = useState<AppLanguage>(() => {
    if (typeof window === 'undefined') {
      return 'en'
    }

    return window.localStorage.getItem(I18N_LANGUAGE_STORAGE_KEY) === 'vi' ? 'vi' : 'en'
  })

  const [dictionaries, setDictionaries] = useState<Record<AppLanguage, TranslationDictionary>>({
    en: {},
    vi: {},
  })

  useEffect(() => {
    let isActive = true

    const loadDictionaries = async (): Promise<void> => {
      try {
        const [enResponse, viResponse] = await Promise.all([
          fetch('/locales/en.json'),
          fetch('/locales/vi.json'),
        ])

        if (!enResponse.ok || !viResponse.ok) {
          throw new Error('Unable to load locale dictionaries.')
        }

        const [enDictionary, viDictionary] = await Promise.all([
          enResponse.json() as Promise<TranslationDictionary>,
          viResponse.json() as Promise<TranslationDictionary>,
        ])

        if (isActive) {
          setDictionaries({
            en: enDictionary,
            vi: viDictionary,
          })
        }
      } catch {
        // Keep fallback labels if locale files are not available.
      }
    }

    void loadDictionaries()

    return () => {
      isActive = false
    }
  }, [])

  useEffect(() => {
    window.localStorage.setItem(I18N_LANGUAGE_STORAGE_KEY, language)
  }, [language])

  const t = useCallback(
    (key: string, fallback: string, params?: TranslationParams): string => {
      const translated = getDictionaryValue(dictionaries[language], key)
      if (translated) {
        return applyTemplateParams(translated, params)
      }

      const englishFallback = getDictionaryValue(dictionaries.en, key)
      if (englishFallback) {
        return applyTemplateParams(englishFallback, params)
      }

      return applyTemplateParams(fallback, params)
    },
    [dictionaries, language]
  )

  const value = useMemo(
    () => ({
      language,
      setLanguage,
      t,
    }),
    [language, t]
  )

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

import { ref } from 'vue'
import { backendErrorKeys, messages, type Locale, type MessageKey } from './messages'

const STORAGE_KEY = 'fc26-locale'

function detectLocale(): Locale {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored === 'zh' || stored === 'en') return stored
  const langs = [navigator.language, ...(navigator.languages ?? [])]
  return langs.some((lang) => lang.toLowerCase().startsWith('zh')) ? 'zh' : 'en'
}

function applyDocument(next: Locale) {
  document.documentElement.lang = next === 'zh' ? 'zh-CN' : 'en'
  document.title = messages[next].documentTitle
}

const locale = ref<Locale>(detectLocale())
applyDocument(locale.value)

export function useI18n() {
  const t = (key: MessageKey, vars?: Record<string, string | number>) => {
    let text: string = messages[locale.value][key]
    if (!vars) return text
    for (const [name, value] of Object.entries(vars)) {
      text = text.replaceAll(`{${name}}`, String(value))
    }
    return text
  }

  const setLocale = (next: Locale) => {
    locale.value = next
    localStorage.setItem(STORAGE_KEY, next)
    applyDocument(next)
  }

  const errorText = (err: unknown, fallback: MessageKey) => {
    const raw = err instanceof Error ? err.message : ''
    if (raw && backendErrorKeys[raw]) return t(backendErrorKeys[raw])
    if (err instanceof TypeError || /fetch|network/i.test(raw) || /No static resource/i.test(raw)) {
      return t('backendOffline')
    }
    if (/^Request failed \(\d+\)$/.test(raw)) return t('requestFailed')
    return t(fallback)
  }

  return {
    locale,
    t,
    setLocale,
    errorText,
  }
}

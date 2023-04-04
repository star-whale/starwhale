import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { locales } from '@/i18n/locales'
import LanguageDetector from 'i18next-browser-languagedetector'
import TimeAgo from 'javascript-time-ago'

import en from 'javascript-time-ago/locale/en.json'
import zh from 'javascript-time-ago/locale/zh.json'

TimeAgo.addDefaultLocale(zh)
TimeAgo.addLocale(en)

i18n.use(LanguageDetector)
    .use(initReactI18next)
    .init({
        resources: {
            en: {
                translations: Object.entries(locales).reduce((p, [k, v]) => {
                    return {
                        ...p,
                        [k]: v.en,
                    }
                }, {}),
            },
            zh: {
                translations: Object.entries(locales).reduce((p, [k, v]) => {
                    return {
                        ...p,
                        [k]: v.zh,
                    }
                }, {}),
            },
        },
        fallbackLng: 'zh',
        debug: false,
        ns: ['translations'],
        defaultNS: 'translations',
        keySeparator: false, // we use content as keys
        interpolation: {
            escapeValue: false,
        },
    })

export default i18n

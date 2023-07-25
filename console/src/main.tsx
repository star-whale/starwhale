import './wdyr'
import React from 'react'
import ReactDOM from 'react-dom/client'
import '@/styles/_global.scss'
import i18n, { initI18n } from '@/i18n'
import reportWebVitals from '@/reportWebVitals'
import App from './App'
import { registerExtensions } from './components/Extensions'
import { registerRoutes } from './routesUtils'
import { registerLocales } from './i18n/locales'
// eslint-disable-next-line
// @ts-ignore
window.g = null
// eslint-disable-next-line
// @ts-ignore
window.i = null

// eslint-disable-next-line
console.log(process.env.GIT_COMMIT_HASH)

async function initExtensions() {
    if (import.meta.env.VITE_EXTENDS === 'true') {
        // @ts-ignore
        // eslint-disable-next-line
        return import('../extensions')
    }

    return {}
}

async function init() {
    // @ts-ignore
    const { authed, unauthed, components, locales } = await initExtensions()

    registerRoutes(authed, unauthed)
    registerExtensions(components)
    registerLocales(locales)
    // should after locale register
    initI18n()

    if (window.location.search.includes('lang=en')) {
        i18n.changeLanguage('en')
    }
    if (window.location.search.includes('lang=zh')) {
        i18n.changeLanguage('zh')
    }

    const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement)

    root.render(<App />)
}

init()
// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals()

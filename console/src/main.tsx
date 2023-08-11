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
// for uno or tailwind
import '@unocss/reset/tailwind.css'
import 'virtual:uno.css'

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
    // init extension routes
    registerRoutes(authed, unauthed)
    // init extension componnets
    registerExtensions(components)
    // init extension locales
    registerLocales(locales)
    // init i18n: should after locale register
    initI18n()
    if (window.location.search.includes('lang=en')) {
        i18n.changeLanguage('en')
    }
    if (window.location.search.includes('lang=zh')) {
        i18n.changeLanguage('zh')
    }
    // init check simple route
    const simple = window.location.pathname.startsWith('/simple')

    const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement)

    root.render(<App simple={simple} />)
}

init()
// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals()

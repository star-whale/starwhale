import './wdyr'
import React from 'react'
import ReactDOM from 'react-dom'
import '@/styles/_global.scss'
import { initI18n } from '@/i18n'
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

    ReactDOM.render(
        <React.StrictMode>
            <App />
        </React.StrictMode>,
        document.getElementById('root')
    )
}

init()
// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals()

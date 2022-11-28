import React from 'react'
import ReactDOM from 'react-dom'
import '@/styles/_global.scss'
import '@/i18n'
import reportWebVitals from '@/reportWebVitals'
import App from './App'

// eslint-disable-next-line
// @ts-ignore
window.g = null
// eslint-disable-next-line
// @ts-ignore
window.i = null
ReactDOM.render(
    <React.StrictMode>
        <App />
    </React.StrictMode>,
    document.getElementById('root')
)

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals()

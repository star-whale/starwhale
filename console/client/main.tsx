import ReactDOM from 'react-dom/client'
import React from 'react'
import App from './App'
import '@starwhale/ui/Markdown/markdown.css'
import '@unocss/reset/tailwind.css'
import 'virtual:uno.css'

const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement)
root.render(<App />)

import * as React from 'react'

async function init() {
    const { default: wdyr } = await import('@welldone-software/why-did-you-render')

    if (window.location.search.indexOf('why-render') !== -1) {
        wdyr(React, {
            include: [/.*/],
            exclude: [/^BrowserRouter/, /^Link/, /^Route/],
            trackHooks: true,
            trackAllPureComponents: true,
        })
    }
}

if (import.meta.env.DEV) {
    init()
}

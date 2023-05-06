import * as React from 'react'

if (import.meta.env.DEV) {
    import('@welldone-software/why-did-you-render').then((m) => {
        if (window.location.search.indexOf('why-render') === -1) return

        m.default(React, {
            include: [/.*/],
            exclude: [/^BrowserRouter/, /^Link/, /^Route/],
            trackHooks: true,
            trackAllPureComponents: true,
        })
    })
}

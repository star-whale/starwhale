/// <reference types="@welldone-software/why-did-you-render" />

import React from 'react'

if (import.meta.env.DEV) {
    if (window.location.search.indexOf('why-render') !== -1) {
        const whyDidYouRender = await import('@welldone-software/why-did-you-render')
        whyDidYouRender.default(React, {
            include: [/.*/],
            exclude: [/^BrowserRouter/, /^Router/, /^Link/, /^Styled/, /^Unknown/, /^WithTheme/, /^Popover/],
            trackHooks: true,
            trackAllPureComponents: true,
        })
    }
}

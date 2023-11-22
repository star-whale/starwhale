import React from 'react'

const RouteInlineContext = React.createContext<{ isInline?: boolean }>({ isInline: false })

const useRouteInlineContext = () => {
    const context = React.useContext(RouteInlineContext)
    return context
}

export { RouteInlineContext, useRouteInlineContext }

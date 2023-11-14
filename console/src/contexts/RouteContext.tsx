import React from 'react'

interface IRouteContext {
    RoutesInline?: React.FC<{ children?: any }>
}

const RouteContext = React.createContext<IRouteContext | undefined>(undefined)

const useRouteContext = () => {
    const context = React.useContext(RouteContext)
    if (!context) throw new Error('useRouteContext must be used within a RouteContext')
    return context
}

export { RouteContext, useRouteContext }

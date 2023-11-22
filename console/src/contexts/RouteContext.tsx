import React from 'react'
import { MemoryRouterProps } from 'react-router'

interface IRouteContext {
    RoutesInline?: React.FC<MemoryRouterProps>
}

const RouteContext = React.createContext<IRouteContext | undefined>(undefined)

const useRouteContext = () => {
    const context = React.useContext(RouteContext)
    if (!context) throw new Error('useRouteContext must be used within a RouteContext')
    return context
}

export { RouteContext, useRouteContext }

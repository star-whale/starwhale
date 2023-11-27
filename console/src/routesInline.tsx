import React from 'react'
import { MemoryRouter, Route, Switch } from 'react-router-dom'
import Pending from '@/pages/Home/Pending'
import { RouteInlineContext } from './contexts/RouteInlineContext'

const RoutesInlineRender = ({ children, routes, ...rest }) => {
    return (
        <React.Suspense fallback={<Pending />}>
            <RouteInlineContext.Provider value={{ isInline: true }}>
                <MemoryRouter {...rest}>
                    <Route>
                        <Switch>{routes}</Switch>
                        {children}
                    </Route>
                </MemoryRouter>
            </RouteInlineContext.Provider>
        </React.Suspense>
    )
}

export default RoutesInlineRender

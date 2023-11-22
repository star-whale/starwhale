import React from 'react'
import { MemoryRouter, Route, Switch } from 'react-router-dom'
import Pending from '@/pages/Home/Pending'
import { RouteInlineContext } from './contexts/RouteInlineContext'

const RoutesInlineRender = ({ children, routes, ...rest }) => {
    return (
        <RouteInlineContext.Provider value={{ isInline: true }}>
            <React.Suspense fallback={<Pending />}>
                <MemoryRouter {...rest}>
                    <Route>
                        <Switch>{routes}</Switch>
                        {children}
                    </Route>
                </MemoryRouter>
            </React.Suspense>
        </RouteInlineContext.Provider>
    )
}

export default RoutesInlineRender

import React from 'react'
import { Switch, Route, Redirect } from 'react-router-dom'
import _ from 'lodash'

export type IRoute = {
    path?: string
    from?: string
    to?: string
    component?: any
    routes?: IRoute[]
}

let unauthedRoutes = {}
let authedRoutes = {}

export function registerRoutes(authed: IRoute, unauthed: IRoute) {
    unauthedRoutes = unauthed
    authedRoutes = authed
}

const renderRoutes = (routes: IRoute[], parent?: IRoute): any => {
    return routes.map((route: any, i: number): any => {
        const key = parent ? `${parent?.path}-${i}` : i
        if (route.from) {
            return <Redirect key={key} exact from={route.from} to={route.to} />
        }
        if (route.element) {
            return React.cloneElement(route.element, { key })
        }
        if (route.component) {
            return (
                <Route
                    key={key}
                    exact
                    path={route.path}
                    render={(props): any => {
                        if (route.routes) {
                            return (
                                <route.component {...props}>
                                    <Switch>{renderRoutes(route.routes, route)}</Switch>
                                </route.component>
                            )
                        }

                        return <route.component {...props} />
                    }}
                />
            )
        }

        return null
    })
}

function mergeRoute(source: IRoute = {}, target: IRoute = {}): JSX.Element {
    const routes: IRoute[] = _.unionWith(target?.routes ?? [], source?.routes ?? [], (a: any, b: any) => {
        if ('path' in a && 'path' in b) {
            return a.path === b.path
        }
        if ('from' in a && 'from' in b) {
            return a.from === b.from
        }
        return false
    }).sort((a, b) => {
        if ('to' in a) return 1
        if ('to' in b) return -1
        return 0
    })

    if (source?.component)
        return (
            <source.component>
                <Switch>{renderRoutes(routes)}</Switch>
            </source.component>
        )

    return renderRoutes(routes)
}

export const getUnauthedRoutes = (source: any) => mergeRoute(source, unauthedRoutes)
export const getAuthedRoutes = (source?: any) => mergeRoute(source, authedRoutes)
export default renderRoutes

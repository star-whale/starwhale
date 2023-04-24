import React from 'react'
import { Switch, Route, Redirect } from 'react-router-dom'
import _ from 'lodash'
// virtual routes created by plugin
import extendRoutes from 'virtual:route-views'
import type { IRoute, IExtendRoutesType } from 'virtual:route-views'

const renderRoutes = (routes: IRoute[], parent?: IRoute): any => {
    return routes.map((route: any, i: number): any => {
        const key = parent ? `${parent?.path}-${i}` : i

        if (route.redirect) {
            return <Redirect key={key} exact from={route.path} to={route.redirect} />
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

export default renderRoutes
function mergeRoute(source: IRoute = {}, target: IExtendRoutesType = {}): JSX.Element {
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

// @FIXME we only support one sample route for now
const unauthedRoutes = extendRoutes?.find((route) => !route.auth)
const authedRoutes = extendRoutes?.find((route) => route.auth)

export const getUnauthedRoutes = (source: any) => mergeRoute(source, unauthedRoutes)
export const getAuthedRoutes = (source?: any) => mergeRoute(source, authedRoutes)

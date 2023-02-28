import React from 'react'
import { Switch, Route } from 'react-router-dom'
import _ from 'lodash'
// virtual routes created by plugin
import routes from 'virtual:route-views'

// @ts-ignore
function mergeRoute(source, target) {
    const routes = _.unionWith(target.routes ?? [], source.routes ?? [], (a: any, b: any) => {
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
    console.log(routes)

    return (
        <source.component>
            <Switch>
                {routes.map((route, i) => (
                    <Route
                        key={i}
                        exact
                        path={route.path}
                        render={(props) =>
                            React.isValidElement(route.element) ? (
                                route.element
                            ) : (
                                <route.component {...props} routes={route.routes} />
                            )
                        }
                    />
                ))}
            </Switch>
        </source.component>
    )
}

// @FIXME we only support one sample route for now
const unauthedRoutes = routes?.find((route) => !route.auth) ?? []

export const getUnauthedRoutes = (source: any) => mergeRoute(source, unauthedRoutes)

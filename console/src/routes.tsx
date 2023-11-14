import React from 'react'
import { BrowserRouter, Switch, Route } from 'react-router-dom'
import { createUseStyles } from 'react-jss'
import ApiHeader from '@/api/ApiHeader'
import Header from '@/components/Header'
import { IThemedStyleProps } from '@starwhale/ui/theme'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import Pending, { NoneBackgroundPending } from '@/pages/Home/Pending'
import { useAuth } from '@/api/Auth'
import { getAuthedRoutes, getUnauthedRoutes } from './routesUtils'
import { unauthed, authed } from './routesMap'
import RoutesInlineRender from './routesInline'
import { RouteContext } from './contexts/RouteContext'

const useStyles = createUseStyles({
    root: ({ theme }: IThemedStyleProps) => ({
        display: 'flex',
        flexFlow: 'column nowrap',
        height: '100vh',
        width: '100vw',
        position: 'relative',
        color: theme.colors.contentPrimary,
    }),
})

const Routes = () => {
    const [, theme] = themedUseStyletron()
    const styles = useStyles({ theme })
    const { token, standaloneMode } = useAuth()
    const unauthedRoutes = getUnauthedRoutes(unauthed)
    const authedRoutes = getAuthedRoutes(authed)

    if (!token) {
        return (
            <React.Suspense fallback={<Pending />}>
                <BrowserRouter>
                    <div className={styles.root}>
                        <Route>
                            <ApiHeader />
                            {unauthedRoutes}
                        </Route>
                    </div>
                </BrowserRouter>
            </React.Suspense>
        )
    }

    const RoutesInline: React.FC<any> = ({ children }) => (
        <RoutesInlineRender routes={authedRoutes}>{children}</RoutesInlineRender>
    )

    return (
        <RouteContext.Provider value={{ RoutesInline }}>
            <React.Suspense fallback={<NoneBackgroundPending />}>
                <BrowserRouter>
                    <div className={styles.root}>
                        <Route>
                            <ApiHeader />
                            {standaloneMode ? null : <Header />}
                            <Switch>{authedRoutes}</Switch>
                        </Route>
                    </div>
                </BrowserRouter>
            </React.Suspense>
        </RouteContext.Provider>
    )
}

export default Routes

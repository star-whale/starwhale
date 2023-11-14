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

    console.log(authedRoutes)

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

    return (
        <React.Suspense fallback={<NoneBackgroundPending />}>
            <BrowserRouter>
                <div className={styles.root}>
                    <Route>
                        <ApiHeader />
                        {standaloneMode ? null : <Header />}
                        <Switch>
                            {/* extends */}
                            {authedRoutes}
                        </Switch>
                    </Route>
                </div>
            </BrowserRouter>
        </React.Suspense>
    )
}

export default Routes

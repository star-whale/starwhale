import React, { useEffect } from 'react'
import { BrowserRouter, Redirect, Route, useHistory, useLocation } from 'react-router-dom'
import { createUseStyles } from 'react-jss'
import { IThemedStyleProps } from '@starwhale/ui/theme'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import Pending from '@/pages/Home/Pending'
import { getUnauthedRoutes } from './routesUtils'
import ReportPreview from '@/pages/Report/ReportPreview'
import CenterLayout from './pages/CenterLayout'
import ApiHeader from './api/ApiHeader'
import Header from './components/Header'

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

const defaultRoutes = {
    auth: false,
    component: CenterLayout,
    routes: [
        {
            path: '/simple/report/preview',
            component: ReportPreview,
        },
        {
            to: '/projects/:path?',
            target: '_blank',
            component: Redirect,
        },
    ],
}

const RedirectComponent = () => {
    const history = useHistory()

    useEffect(() => {
        history?.listen((location) => {
            if (!location.pathname.startsWith('simple')) {
                window.location.reload()
            }
        })
    }, [history])

    return null
}

const Routes = () => {
    const [, theme] = themedUseStyletron()
    const styles = useStyles({ theme })
    const unauthedRoutes = getUnauthedRoutes(defaultRoutes)

    return (
        <React.Suspense fallback={<Pending />}>
            <BrowserRouter>
                <div className={styles.root}>
                    <Route>
                        <ApiHeader />
                        <Header simple={true} />
                        {unauthedRoutes}
                        <RedirectComponent />
                    </Route>
                </div>
            </BrowserRouter>
        </React.Suspense>
    )
}

export default Routes

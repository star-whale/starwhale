// @flow
import React from 'react'
import { Client as Styletron } from 'styletron-engine-atomic'
import { Provider as StyletronProvider } from 'styletron-react'
import { BaseProvider } from 'baseui'
import { SidebarContext } from '@/contexts/SidebarContext'
import { useSidebar } from '@/hooks/useSidebar'
import { QueryClient, QueryClientProvider } from 'react-query'
import themes from '@/theme'
import { apiInit } from '@/api'
import { ToasterContainer } from 'baseui/toast'
import { ConfirmCtxProvider } from '@/components/Modal/confirm'
import '@/assets/fonts/iconfont.css'
import Routes from './routes'
import { AuthProvider } from './api/Auth'

apiInit()

const engine = new Styletron()
const queryClient = new QueryClient()
export default function App(): any {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const sidebarData = useSidebar()
    // const themeType = useCurrentThemeType()
    const theme = themes.deep

    return (
        <QueryClientProvider client={queryClient}>
            <StyletronProvider value={engine}>
                <BaseProvider theme={theme}>
                    <AuthProvider>
                        <ToasterContainer autoHideDuration={3000} />
                        <ConfirmCtxProvider>
                            <SidebarContext.Provider value={sidebarData}>
                                <Routes />
                            </SidebarContext.Provider>
                        </ConfirmCtxProvider>
                    </AuthProvider>
                </BaseProvider>
            </StyletronProvider>
        </QueryClientProvider>
    )
}

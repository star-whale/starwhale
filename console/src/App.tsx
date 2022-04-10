//@flow
import React from 'react'
import { Client as Styletron } from 'styletron-engine-atomic'
import { Provider as StyletronProvider } from 'styletron-react'
import { BaseProvider } from 'baseui'
import { SidebarContext } from '@/contexts/SidebarContext'
import { useSidebar } from '@/hooks/useSidebar'
import Routes from '@/routes'
import { QueryClient, QueryClientProvider } from 'react-query'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import themes from '@/theme'
import { apiInit } from '@/api'
import { ToasterContainer } from 'baseui/toast'

apiInit()
const engine = new Styletron()
const queryClient = new QueryClient()

export default function App(): any {
    const sidebarData = useSidebar()
    const themeType = useCurrentThemeType()
    const theme = themes[themeType]

    return (
        <QueryClientProvider client={queryClient}>
            <StyletronProvider value={engine}>
                <BaseProvider theme={theme}>
                    <ToasterContainer autoHideDuration={3000} />
                    <SidebarContext.Provider value={sidebarData}>
                        <Routes />
                    </SidebarContext.Provider>
                </BaseProvider>
            </StyletronProvider>
        </QueryClientProvider>
    )
}

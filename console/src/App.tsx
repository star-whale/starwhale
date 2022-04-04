// @flow
import React, { useEffect } from 'react'
import { Client as Styletron } from 'styletron-engine-atomic'
import { Provider as StyletronProvider } from 'styletron-react'
import { BaseProvider, DarkTheme, createTheme } from 'baseui'
import type { LightTheme as LightThemeType } from 'baseui'
import { ToasterContainer } from 'baseui/toast'
import { SidebarContext } from '@/contexts/SidebarContext'
import { useSidebar } from '@/hooks/useSidebar'
import Routes from '@/routes'
import { QueryClient, QueryClientProvider } from 'react-query'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'

const engine = new Styletron()
const queryClient = new QueryClient()
// todo refact
const primitives = {
    primaryFontFamily: 'Inter',
}
const overrides = {
    colors: {
        buttonPrimaryFill: '#007FFF',
        // buttonPrimaryHover: '#FFFFFF',
        buttonBorderRadius: '14px',
    },
    typography: {},
}
const theme = createTheme(primitives, overrides)

export default function Hello() {
    const sidebarData = useSidebar()
    const themeType = useCurrentThemeType()

    return (
        <QueryClientProvider client={queryClient}>
            <StyletronProvider value={engine}>
                <BaseProvider theme={themeType === 'dark' ? DarkTheme : theme}>
                    <ToasterContainer autoHideDuration={5000}>
                        <SidebarContext.Provider value={sidebarData}>
                            <Routes />
                        </SidebarContext.Provider>
                    </ToasterContainer>
                </BaseProvider>
            </StyletronProvider>
        </QueryClientProvider>
    )
}

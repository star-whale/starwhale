import React from 'react'
import { Client as Styletron } from 'styletron-engine-atomic'
import { Provider as StyletronProvider } from 'styletron-react'
import { BaseProvider, LocaleProvider } from 'baseui'
import { SidebarContext } from '@/contexts/SidebarContext'
import { useSidebar } from '@/hooks/useSidebar'
import { QueryClient, QueryClientProvider } from 'react-query'
import DeepTheme from '@starwhale/ui/theme'
import { apiInit } from '@/api'
import { ToasterContainer } from 'baseui/toast'
import { ConfirmCtxProvider } from '@starwhale/ui/Modal'
import Routes from './routes'
import { AuthProvider } from './api/Auth'
import i18n from './i18n'
import locales from '@starwhale/ui/i18n'

apiInit()

const engine = new Styletron()
const queryClient = new QueryClient()
export default function App(): any {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const sidebarData = useSidebar()

    const overrideLanguage = React.useMemo(() => {
        // @ts-ignore
        return locales?.[i18n.language] ?? {}
    }, [])

    return (
        <QueryClientProvider client={queryClient}>
            <StyletronProvider value={engine}>
                <BaseProvider theme={DeepTheme}>
                    <LocaleProvider locale={overrideLanguage}>
                        <AuthProvider>
                            <ToasterContainer autoHideDuration={3000} />
                            <ConfirmCtxProvider>
                                <SidebarContext.Provider value={sidebarData}>
                                    <Routes />
                                </SidebarContext.Provider>
                            </ConfirmCtxProvider>
                        </AuthProvider>
                    </LocaleProvider>
                </BaseProvider>
            </StyletronProvider>
        </QueryClientProvider>
    )
}
// App.whyDidYouRender = true

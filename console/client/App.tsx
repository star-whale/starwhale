import React from 'react'
import { Provider as StyletronProvider } from 'styletron-react'
import { BaseProvider, LocaleProvider } from 'baseui'
import DeepTheme from '@starwhale/ui/theme'
import { Client as Styletron } from 'styletron-engine-atomic'
import { QueryClient, QueryClientProvider } from 'react-query'
import ServingPage from './ServingPage'
import { initI18n } from '@/i18n'

initI18n()

export default function App(): any {
    return (
        <QueryClientProvider client={new QueryClient()}>
            <StyletronProvider value={new Styletron()}>
                <BaseProvider theme={DeepTheme}>
                    <LocaleProvider locale={{}}>
                        <ServingPage />
                    </LocaleProvider>
                </BaseProvider>
            </StyletronProvider>
        </QueryClientProvider>
    )
}

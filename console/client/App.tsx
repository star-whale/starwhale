import React from 'react'
import { Provider as StyletronProvider } from 'styletron-react'
import { BaseProvider } from 'baseui'
import DeepTheme from '@starwhale/ui/theme'
import { Client as Styletron } from 'styletron-engine-atomic'
import { QueryClient, QueryClientProvider } from 'react-query'
import ServingPage from './ServingPage'

export default function App(): any {
    return (
        <StyletronProvider value={new Styletron()}>
            <BaseProvider theme={DeepTheme}>
                <QueryClientProvider client={new QueryClient()}>
                    <ServingPage />
                </QueryClientProvider>
            </BaseProvider>
        </StyletronProvider>
    )
}

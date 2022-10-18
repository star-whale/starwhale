import * as React from 'react'
import { Provider as StyletronProvider } from 'styletron-react'
import { Client as Styletron } from 'styletron-engine-atomic'
import { BaseProvider } from 'baseui'
import themes from '@/theme'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from 'react-query'
import '@/i18n'
import { render, RenderOptions } from '@testing-library/react'

const engine = new Styletron()
const queryClient = new QueryClient()

export const WithAll = ({ children }: { children?: React.ReactNode }) => {
    return (
        <QueryClientProvider client={queryClient}>
            <StyletronProvider value={engine}>
                <BaseProvider theme={themes.deep}>
                    <BrowserRouter>{children}</BrowserRouter>
                </BaseProvider>
            </StyletronProvider>
        </QueryClientProvider>
    )
}

export function TestBaseProvider({ children }: { children?: React.ReactNode }) {
    return <BaseProvider theme={themes.deep}>{children}</BaseProvider>
}

export const routeRender = (ui: React.ReactElement, options?: Omit<RenderOptions, 'wrapper'>) =>
    render(ui, { wrapper: WithAll, ...options })

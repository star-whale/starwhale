import React from 'react'
import { Client as Styletron } from 'styletron-engine-atomic'
import { Provider as StyletronProvider } from 'styletron-react'
import { BaseProvider } from 'baseui'
import { QueryClient, QueryClientProvider } from 'react-query'
import { BrowserRouter } from 'react-router-dom'
import DeepTheme from '@starwhale/ui/theme'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { ToasterContainer } from 'baseui/toast'
import { createUseStyles } from 'react-jss'

import '@/styles/_global.scss'

const engine = new Styletron()
const queryClient = new QueryClient()

const useStyles = createUseStyles({
    root: ({ theme }) => ({
        color: theme.colors.contentPrimary,
    }),
})

const Story = ({ storyFn }) => storyFn()

const ThemeDecorator = (storyFn) => {
    const [, theme] = themedUseStyletron()
    const styles = useStyles({ theme })

    return (
        <React.StrictMode>
            <QueryClientProvider client={queryClient}>
                <StyletronProvider value={engine}>
                    <BaseProvider theme={DeepTheme}>
                        <ToasterContainer autoHideDuration={3000} />
                        <BrowserRouter>
                            <div className={styles.root}>
                                <Story storyFn={storyFn} />
                            </div>
                        </BrowserRouter>
                    </BaseProvider>
                </StyletronProvider>
            </QueryClientProvider>
        </React.StrictMode>
    )
}

export const decorators = [ThemeDecorator]

export const parameters = {
    actions: { argTypesRegex: '^on[A-Z].*' },
    controls: {
        matchers: {
            color: /(background|color)$/i,
            date: /Date$/,
        },
    },
}

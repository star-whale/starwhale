import React from 'react'
import { Client as Styletron } from 'styletron-engine-atomic'
import { Provider as StyletronProvider } from 'styletron-react'
import { LightTheme, BaseProvider, DarkTheme } from 'baseui'
import { QueryClient, QueryClientProvider } from 'react-query'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import { BrowserRouter, Switch, Route } from 'react-router-dom'
import themes, { IThemedStyleProps } from '@/theme'
import { ToasterContainer } from 'baseui/toast'
import { createUseStyles } from 'react-jss'

import '@/styles/_global.scss'

const engine = new Styletron()
const queryClient = new QueryClient()

const useStyles = createUseStyles({
    root: ({ theme }) => ({
        // background: 'var(--color-brandRootBackground)',
        color: 'var(--color-contentPrimary)',
        ...Object.entries(theme.colors).reduce((p, [k, v]) => {
            return {
                ...p,
                [`--color-${k}`]: v,
            }
        }, {}),
    }),
})
const ThemeDecorator = (storyFn) => {
    const themeType = useCurrentThemeType()
    const theme = themes[themeType]
    const styles = useStyles({ theme, themeType })

    return (
        <QueryClientProvider client={queryClient}>
            <StyletronProvider value={engine}>
                <BaseProvider theme={theme}>
                    <ToasterContainer autoHideDuration={3000} />
                    <BrowserRouter>
                        <div className={styles.root}>{storyFn()}</div>
                    </BrowserRouter>
                </BaseProvider>
            </StyletronProvider>
        </QueryClientProvider>
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

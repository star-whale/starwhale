
import React from 'react'
import { Client as Styletron } from 'styletron-engine-atomic'
import { Provider as StyletronProvider } from 'styletron-react'
import { LightTheme, BaseProvider, DarkTheme } from 'baseui'
import { QueryClient, QueryClientProvider } from 'react-query'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import { BrowserRouter, Switch, Route } from 'react-router-dom'

const engine = new Styletron()
const queryClient = new QueryClient()

const ThemeDecorator = storyFn => {
    const themeType = useCurrentThemeType()

    return (
        <QueryClientProvider client={queryClient}>
            <StyletronProvider value={engine}>
                <BaseProvider theme={themeType === 'dark' ? DarkTheme : LightTheme}>
                  <BrowserRouter>
                    {storyFn()}
                  </BrowserRouter>                   
                </BaseProvider>
            </StyletronProvider>
        </QueryClientProvider>
    )
}

export const decorators  = [ThemeDecorator]

export const parameters = {
  actions: { argTypesRegex: "^on[A-Z].*" },
  controls: {
    matchers: {
      color: /(background|color)$/i,
      date: /Date$/,
    },
  },
}

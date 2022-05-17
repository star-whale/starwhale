import { Theme } from 'baseui/theme'
import color from 'color'
import { createDarkTheme, createLightTheme, DarkTheme as BaseDarkTheme } from 'baseui'

export type BaseThemeType = 'light' | 'dark' | 'deep'
export type ThemeType = BaseThemeType | 'followTheSystem'

export interface IThemedStyleProps {
    theme: Theme
    themeType: BaseThemeType
}

export interface IComposedComponentProps {
    style?: React.CSSProperties
    className?: string
}

export const colors = {
    brandPrimary: '#2B65D9',
    brandPrimaryHover: color('#2B65D9').alpha(0.5).toString(),
    brandFontPrimary: '#02102B',
    brandBackgroundPrimar: '#EBF1FF',
    brandBgNav: '#122A59',
    brandBgNavTitle: '#1D3973',
    brandBgNavBorder: '#1D3973',
    brandBgNavFont: '#FFFFFF',
    brandBgNavFontGray: color('#FFFFFF').alpha(0.6).toString(),
    brandBgContent: '#EBF1FF',
    brandBgUser: '#D0DDF7',
    brandBgSecondory: '#D0DDF7',
    brandBgSecondory4: color('#D0DDF7').alpha(0.4).toString(),
    brandBgUserFont: '#02102B',
    brandTableHeaderBackground: '#F3F5F9',
    brandLink: '#2B65D9',
}

const primitives = {
    // primaryFontFamily: 'Consolas',
}

const overrides = {
    light: {
        colors: {
            // ----------- custom -----------
            brandRootBackground: '#fdfdfd',
        },
    },
    dark: {
        colors: {
            // ----------- custom -----------
            brandRootBackground: BaseDarkTheme.colors.backgroundPrimary,
            brandHeaderBackground: color(BaseDarkTheme.colors.backgroundPrimary).fade(0.5).string(),
        },
    },
    deep: {
        colors: {
            buttonPrimaryFill: colors.brandPrimary,
            // ----------- custom -----------
            brandRootBackground: colors.brandBackgroundPrimar,
            ...colors,
        },
        typography: {},
        borders: {
            buttonBorderRadius: '4px',
        },
    },
}

export const DeepTheme: Theme = createLightTheme(primitives, overrides.deep)
export const LightTheme: Theme = createLightTheme(primitives, overrides.light)
export const DarkTheme: Theme = createDarkTheme(primitives, overrides.dark)

export default {
    light: LightTheme,
    dark: DarkTheme,
    deep: DeepTheme,
} as { [key in BaseThemeType]: Theme }

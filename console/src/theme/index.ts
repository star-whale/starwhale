import { Theme } from 'baseui/theme'
import color from 'color'
import { createDarkTheme, createLightTheme, DarkTheme as BaseDarkTheme } from 'baseui'

export type BaseThemeType = 'light' | 'dark' | 'deep'
export type ThemeType = BaseThemeType | 'followTheSystem'

export interface IThemedStyleProps {
    theme: Theme
    themeType: BaseThemeType
}

// TODO: type support
// export interface IColors extends Partial<Colors>  {
//     brand1: string
//     brand2: string
//     brandBackground: string
// }

export const colors = {
    brand1: '#273343',
    brand2: '#0C1B3E',
    brandBackground1: '#F1F6FF',
    brandBackground2: '#F3F6F9',
    brandBackground3: '#E7EBF0',
    brandBackground4: '#E0E3E7',
    brandFontRmphasis: '#1A2027',
    brandFontRegular: '#2D3843',
    brandFontCaption: '#3E5060',
    brandFontDisable: '#BEC2C2',
    brandIndicatorAction1: '#A0AAB4',
    brandIndicatorRegular: '#BFC7CF',
    brandIndicatorDisabled: color('#000000').alpha(2.5).toString(),
    brandIndicatorCaption: color('#000000').alpha(4.5).toString(),
    brandIndicatorSuccess: '#34B576',
    brandIndicatorError: '#E82037',
    brandIndicatorWarning: '#FFCD00',
    brandIndicatorSuspend: '#722ED1',
    brandIndicatorCompleted: '#91D5FF',
    brandLink: '#009BDE',
}

const primitives = {
    primaryFontFamily: 'Consolas',
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
            buttonPrimaryFill: '#273343',
            // ----------- custom -----------
            brandRootBackground: colors.brandBackground1,
            brandHeaderBackground: colors.brand2,
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

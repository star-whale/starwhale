import { Theme } from 'baseui/theme'
import color from 'color'
import { createDarkTheme, createLightTheme, LightTheme as BaseLightTheme, DarkTheme as BaseDarkTheme } from 'baseui'

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

const customPrimiaryColors = {
    primary: '#2B65D9',
    primaryHover: '#5181E0',
    primaryPressed: '#1C4CAD',
    backgroundPrimary: '#EBF1FF',
    backgroundSecondary: '#D0DDF7',
    backgroundHover: '#F0F4FF',
    backgroundFullscreen: color('#02102B').alpha(0.5).toString(),
    backgroundNav: ' #122A59',
    backgroundNavFixed: ' #1D3973;',
    dividerPrimary: ' #EEF1F6',
    dividerSecondary: '  #CFD7E6',
    fontPrimay: '#02102B',
    fontNote: color('#02102B').alpha(0.6).toString(),
    fontTip: color('#02102B').alpha(0.4).toString(),
    fontDisable: color('#02102B').alpha(0.2).toString(),
    fontWhite: '#FFFFFF',
    fontWhite60: color('#FFFFFF').alpha(0.6).toString(),
    error: '#CC3D3D',
    success: '#00B368',
    warning: '#E67F17',
    tips: '#4D576A',
    shadow1: BaseLightTheme.lighting.shadow500,
    shadow2: BaseLightTheme.lighting.shadow600,
}

const primitives = {
    primaryFontFamily: 'Cousine',
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
            primary: customPrimiaryColors.primary,
            accent: customPrimiaryColors.primary,
            positive: customPrimiaryColors.primary,
            contentPrimary: customPrimiaryColors.fontPrimay,
            buttonPrimaryFill: customPrimiaryColors.primary,
            buttonPrimaryHover: customPrimiaryColors.primaryHover,
            buttonPrimaryActive: customPrimiaryColors.primaryPressed,
            borderSelected: customPrimiaryColors.primary,
            tickFillSelected: customPrimiaryColors.primary,
            tickFillSelectedHover: customPrimiaryColors.primary,
            // ----------- custom -----------
            brandPrimary: customPrimiaryColors.primary,
            brandPrimaryHover: customPrimiaryColors.primaryHover,
            brandFontPrimary: customPrimiaryColors.fontPrimay,
            brandBgNav: customPrimiaryColors.backgroundNav,
            brandBgNavTitle: customPrimiaryColors.backgroundNavFixed,
            brandBgNavBorder: customPrimiaryColors.backgroundNavFixed,
            brandBgNavFont: customPrimiaryColors.fontWhite,
            brandBgNavFontGray: customPrimiaryColors.fontWhite60,
            brandBgUser: customPrimiaryColors.backgroundSecondary,
            brandBgSecondory: customPrimiaryColors.backgroundSecondary,
            brandBgSecondory4: color('#D0DDF7').alpha(0.4).toString(),
            brandBgUserFont: customPrimiaryColors.fontPrimay,
            brandTableHeaderBackground: '#F3F5F9',
            brandLink: customPrimiaryColors.primary,
            brandRootBackground: customPrimiaryColors.backgroundPrimary,
            brandLoginBackground: customPrimiaryColors.backgroundNav,
            brandFontTip: customPrimiaryColors.fontTip,
            brandFontWhite: customPrimiaryColors.fontWhite,
            brandFontNote: customPrimiaryColors.fontNote,
            brandWhite: customPrimiaryColors.fontWhite,
            brandUserIcon: customPrimiaryColors.backgroundSecondary,
            brandMenuItemBackground: customPrimiaryColors.backgroundHover,
        },
        typography: {},
        borders: {
            inputBorderRadius: '4px',
            buttonBorderRadius: '4px',
            popoverBorderRadius: '4px',
            surfaceBorderRadius: '4px',
            tagBorderRadius: '4px',
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

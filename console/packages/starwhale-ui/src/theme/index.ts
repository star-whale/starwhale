import type { Theme, Primitives } from 'baseui'
import color from 'color'
import { createTheme, LightTheme } from 'baseui'
import { Borders, Colors, Typography } from 'baseui/styles'

export type BaseThemeType = 'deep'
export type ThemeType = BaseThemeType | 'followTheSystem'

export interface IComposedComponentProps {
    style?: React.CSSProperties
    className?: string
}

const customPrimaryColors = {
    primary: '#2B65D9',
    primaryHover: '#5181E0',
    primaryPressed: '#1C4CAD',
    secondary: '#EBF1FF',
    secondaryHover: '#D1E0FF',
    secondaryPressed: '#B8D0FF',
    tertiary: '#F5F8FF',
    backgroundPrimary: '#EBF1FF',
    backgroundSecondary: '#D0DDF7',
    backgroundHover: '#F0F4FF',
    backgroundFullscreen: color('#02102B').alpha(0.5).toString(),
    backgroundNav: ' #122A59',
    backgroundNavFixed: ' #1D3973',
    dividerPrimary: ' #EEF1F6',
    dividerSecondary: '#CFD7E6',
    fontPrimary: '#02102B',
    fontNote: color('#02102B').alpha(0.6).toString(),
    fontTip: color('#02102B').alpha(0.4).toString(),
    fontDisable: color('#02102B').alpha(0.2).toString(),
    fontWhite: '#FFFFFF',
    fontWhite60: color('#FFFFFF').alpha(0.6).toString(),
    error: '#CC3D3D',
    errorBackground: '#FFEDED',
    success: '#00B368',
    successBackground: '#E6FFF4',
    warning: '#E67F17',
    warningBackground: '#FFF3E8',
    tips: '#4D576A',
    tipsBackground: '#F0F5FF',
    fill: '#FFF',
    shadow1: LightTheme.lighting.shadow500,
    shadow2: LightTheme.lighting.shadow600,
}

const primitives: Partial<Primitives> = {
    primaryFontFamily: 'Inter',
}

const overrides: {
    colors: Partial<Colors>
    typography: Partial<Typography>
    borders: Partial<Borders>
} = {
    colors: {
        primary: customPrimaryColors.primary,
        accent: customPrimaryColors.primary,
        positive: customPrimaryColors.primary,
        contentPrimary: customPrimaryColors.fontPrimary,
        // ----------- button -----------
        buttonPrimaryFill: customPrimaryColors.primary,
        buttonPrimaryHover: customPrimaryColors.primaryHover,
        buttonPrimaryActive: customPrimaryColors.primaryPressed,
        buttonSecondaryFill: customPrimaryColors.secondary,
        buttonSecondaryHover: customPrimaryColors.secondaryHover,
        buttonSecondaryActive: customPrimaryColors.secondaryPressed,
        buttonSecondaryText: customPrimaryColors.primary,
        buttonSecondarySelectedText: customPrimaryColors.primaryPressed,
        buttonSecondarySelectedFill: customPrimaryColors.primaryPressed,
        buttonTertiaryFill: customPrimaryColors.tertiary,
        buttonTertiaryHover: customPrimaryColors.secondaryHover,
        buttonTertiaryActive: customPrimaryColors.secondaryPressed,
        buttonTertiaryText: customPrimaryColors.primary,
        buttonDisabledFill: customPrimaryColors.secondary,
        buttonDisabledText: color(customPrimaryColors.primary).alpha(0.3).toString(),
        calendarDayBackgroundSelectedHighlighted: customPrimaryColors.primary,
        calendarDayBackgroundPseudoSelectedHighlighted: customPrimaryColors.secondary,
        // ----------- others -----------
        borderSelected: customPrimaryColors.primary,
        tickBorder: customPrimaryColors.dividerSecondary,
        tickFill: customPrimaryColors.fill,
        tickFillActive: customPrimaryColors.fill,
        tickFillSelected: customPrimaryColors.primary,
        tickFillSelectedHover: customPrimaryColors.primary,
        tickFillSelectedHoverActive: customPrimaryColors.primary,
        inputFillError: customPrimaryColors.fill,
        inputBorder: customPrimaryColors.dividerSecondary,
        inputFill: customPrimaryColors.fill,
        inputFillActive: customPrimaryColors.fill,
        inputBorderError: customPrimaryColors.error,
        toastInfoText: customPrimaryColors.tips,
        toastInfoBackground: customPrimaryColors.tipsBackground,
        toastPositiveText: customPrimaryColors.success,
        toastPositiveBackground: customPrimaryColors.successBackground,
        toastWarningText: customPrimaryColors.warning,
        toastWarningBackground: customPrimaryColors.warningBackground,
        toastNegativeText: customPrimaryColors.error,
        toastNegativeBackground: customPrimaryColors.errorBackground,
        notificationInfoText: customPrimaryColors.tips,
        notificationInfoBackground: customPrimaryColors.tipsBackground,
        notificationPositiveText: customPrimaryColors.success,
        notificationPositiveBackground: customPrimaryColors.successBackground,
        notificationWarningText: customPrimaryColors.warning,
        notificationWarningBackground: customPrimaryColors.warningBackground,
        notificationNegativeText: customPrimaryColors.error,
        notificationNegativeBackground: customPrimaryColors.errorBackground,
        menuFillHover: customPrimaryColors.backgroundPrimary,
    },
    typography: {
        // @ts-ignore
        font250: {
            lineHeight: '14px',
        },
    },
    borders: {
        inputBorderRadius: '4px',
        buttonBorderRadius: '4px',
        popoverBorderRadius: '4px',
        surfaceBorderRadius: '4px',
        tagBorderRadius: '4px',
    },
}

export const themeOverrided: Theme = createTheme(primitives, overrides)

const SWTheme = {
    ...themeOverrided,
    // ----------- custom -----------
    brandPrimary: customPrimaryColors.primary,
    brandPrimaryHover: customPrimaryColors.primaryHover,
    brandFontPrimary: customPrimaryColors.fontPrimary,
    brandBgNav: customPrimaryColors.backgroundNav,
    brandBgNavTitle: customPrimaryColors.backgroundNavFixed,
    brandBgNavBorder: customPrimaryColors.backgroundNavFixed,
    brandBgNavFont: customPrimaryColors.fontWhite,
    brandBgNavFontGray: customPrimaryColors.fontWhite60,
    brandBgUser: customPrimaryColors.backgroundSecondary,
    brandBgSecondary: customPrimaryColors.backgroundSecondary,
    brandBgUserFont: customPrimaryColors.fontPrimary,
    brandLink: customPrimaryColors.primary,
    brandRootBackground: customPrimaryColors.backgroundPrimary,
    brandLoginBackground: customPrimaryColors.backgroundNav,
    brandFontTip: customPrimaryColors.fontTip,
    brandFontWhite: customPrimaryColors.fontWhite,
    brandFontNote: customPrimaryColors.fontNote,
    brandWhite: customPrimaryColors.fontWhite,
    brandUserIcon: customPrimaryColors.backgroundSecondary,
    brandMenuItemBackground: customPrimaryColors.backgroundHover,
    brandTableHeaderBackground: '#F3F5F9',
    brandTableHeaderBackgroundHover: color(customPrimaryColors.dividerSecondary).alpha(0.3).toString(),
    brandTableHeaderResizer: customPrimaryColors.dividerSecondary,
}
export type SWThemeT = typeof SWTheme

export interface IThemedStyleProps {
    theme: SWThemeT
    themeType?: BaseThemeType
}

export default SWTheme

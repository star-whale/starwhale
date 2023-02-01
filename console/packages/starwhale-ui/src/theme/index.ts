import type { Theme, Primitives } from 'baseui'
import color from 'color'
import { createTheme, LightTheme } from 'baseui'

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
    backgroundPrimary: '#EBF1FF',
    backgroundSecondary: '#D0DDF7',
    backgroundHover: '#F0F4FF',
    backgroundFullscreen: color('#02102B').alpha(0.5).toString(),
    backgroundNav: ' #122A59',
    backgroundNavFixed: ' #1D3973',
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
    shadow1: LightTheme.lighting.shadow500,
    shadow2: LightTheme.lighting.shadow600,
}

const primitives: Partial<Primitives> = {
    primaryFontFamily: 'Source Sans Pro',
}

const overrides = {
    colors: {
        primary: customPrimaryColors.primary,
        accent: customPrimaryColors.primary,
        positive: customPrimaryColors.primary,
        contentPrimary: customPrimaryColors.fontPrimay,
        buttonPrimaryFill: customPrimaryColors.primary,
        buttonPrimaryHover: customPrimaryColors.primaryHover,
        buttonPrimaryActive: customPrimaryColors.primaryPressed,
        buttonSecondaryFill: '#F4F5F7',
        buttonSecondaryText: 'rgba(2,16,43,0.60)',
        buttonSecondarySelectedText: '#1C4CAD',
        buttonSecondarySelectedFill: '#1C4CAD',
        buttonSecondaryHover: '#F0F4FF',
        buttonSecondaryActive: '#F0F4FF;',
        buttonDisabledFill: '#EBF1FF',
        buttonDisabledText: color(customPrimaryColors.primary).alpha(0.3).toString(),
        borderSelected: customPrimaryColors.primary,
        tickFillSelected: customPrimaryColors.primary,
        tickFillSelectedHover: customPrimaryColors.primary,
        inputBorder: customPrimaryColors.dividerSecondary,
        inputFill: '#FFF',
        inputFillActive: '#FFF',
        borderFocus: customPrimaryColors.primary,
        toastInfoText: '#4D576A',
        toastInfoBackground: '#F0F5FF',
        toastPositiveText: ' #00B368',
        toastPositiveBackground: ' #E6FFF4',
        toastWarningText: '#E67F17',
        toastWarningBackground: '#FFF3E8',
        toastNegativeText: '#CC3D3D',
        toastNegativeBackground: '#FFEDED',
        notificationInfoText: '#4D576A',
        notificationInfoBackground: '#F0F5FF',
        notificationPositiveText: ' #00B368',
        notificationPositiveBackground: ' #E6FFF4',
        notificationWarningText: '#E67F17',
        notificationWarningBackground: '#FFF3E8',
        notificationNegativeText: '#CC3D3D',
        notificationNegativeBackground: '#FFEDED',
    },
    typography: {
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
    brandFontPrimary: customPrimaryColors.fontPrimay,
    brandBgNav: customPrimaryColors.backgroundNav,
    brandBgNavTitle: customPrimaryColors.backgroundNavFixed,
    brandBgNavBorder: customPrimaryColors.backgroundNavFixed,
    brandBgNavFont: customPrimaryColors.fontWhite,
    brandBgNavFontGray: customPrimaryColors.fontWhite60,
    brandBgUser: customPrimaryColors.backgroundSecondary,
    brandBgSecondary: customPrimaryColors.backgroundSecondary,
    brandBgSecondary4: color('#D0DDF7').alpha(0.4).toString(),
    brandBgUserFont: customPrimaryColors.fontPrimay,
    brandTableHeaderBackground: '#F3F5F9',
    brandLink: customPrimaryColors.primary,
    brandRootBackground: customPrimaryColors.backgroundPrimary,
    brandLoginBackground: customPrimaryColors.backgroundNav,
    brandFontTip: customPrimaryColors.fontTip,
    brandFontWhite: customPrimaryColors.fontWhite,
    brandFontNote: customPrimaryColors.fontNote,
    brandWhite: customPrimaryColors.fontWhite,
    brandUserIcon: customPrimaryColors.backgroundSecondary,
    brandMenuItemBackground: customPrimaryColors.backgroundHover,
}
export type SWThemeT = typeof SWTheme

export interface IThemedStyleProps {
    theme: SWThemeT
    themeType?: BaseThemeType
}

export default SWTheme

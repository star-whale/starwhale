/// <reference types="react" />
import { Theme } from 'baseui/theme';
export declare type BaseThemeType = 'light' | 'dark' | 'deep';
export declare type ThemeType = BaseThemeType | 'followTheSystem';
export interface IThemedStyleProps {
    theme: Theme;
    themeType: BaseThemeType;
}
export interface IComposedComponentProps {
    style?: React.CSSProperties;
    className?: string;
}
export declare const DeepTheme: Theme;
export declare const LightTheme: Theme;
export declare const DarkTheme: Theme;
declare const _default: {
    dark: Theme;
    light: Theme;
    deep: Theme;
};
export default _default;

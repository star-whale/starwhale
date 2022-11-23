import React from 'react';
declare const iconTypes: string[];
interface IIconFontProps {
    style?: React.CSSProperties;
    size?: number;
    kind?: 'inherit' | 'white' | 'gray' | 'white2' | 'primary';
    type: typeof iconTypes[number];
}
export default function IconFont({ size, type, kind, style }: IIconFontProps): JSX.Element;
export {};

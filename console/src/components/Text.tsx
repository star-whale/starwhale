import React from 'react'
import { useStyletron } from 'baseui'

export interface ITextProps {
    children: React.ReactNode
    size?: 'small' | 'medium' | 'large' | 'xlarge'
    style?: React.CSSProperties
}

const fontSizeMap: { [k in Exclude<ITextProps['size'], undefined>]: string } = {
    small: '12px',
    medium: '14px',
    large: '16px',
    xlarge: '20px',
}

export default function Text({ children, style, size = 'medium' }: ITextProps) {
    const [css, theme] = useStyletron()
    return (
        <span
            style={style}
            className={css({
                fontSize: fontSizeMap[size],
            })}
        >
            {children}
        </span>
    )
}

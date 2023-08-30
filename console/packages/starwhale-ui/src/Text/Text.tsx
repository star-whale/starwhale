import React from 'react'
import { Tooltip } from '../Tooltip'
import { IconCopy } from '../Copy'
import { themedUseStyletron } from '../theme/styletron'

export interface ITextProps {
    children?: React.ReactNode
    size?: 'small' | 'medium' | 'large' | 'xlarge'
    style?: React.CSSProperties
    tooltip?: React.ReactNode
    content?: string
    maxWidth?: string
}

const fontSizeMap: { [k in Exclude<ITextProps['size'], undefined>]: string } = {
    small: '12px',
    medium: '14px',
    large: '16px',
    xlarge: '20px',
}

function Text({ children, style, size = 'medium', tooltip = '', content = '', maxWidth = '200px' }: ITextProps) {
    const [css] = themedUseStyletron()

    let Component = (
        <span
            style={style}
            className={css({
                fontSize: fontSizeMap[size],
                WebkitLineClamp: 1,
                display: 'inline-block',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                verticalAlign: 'middle',
                maxWidth,
            })}
        >
            {children ?? content}
        </span>
    )

    if (tooltip) {
        Component = (
            <Tooltip content={tooltip} showArrow placement='top'>
                {Component}
            </Tooltip>
        )
    }

    return (
        <>
            {Component}
            {content && <IconCopy content={content} />}
        </>
    )
}

function MonoText({ children, style, ...props }: ITextProps) {
    return (
        <Text {...props} style={{ ...style, fontFamily: 'Roboto Mono' }}>
            {children}
        </Text>
    )
}

function VersionText({ version, style, ...props }: ITextProps & { version?: string }) {
    if (!version) return null
    if (typeof version !== 'string') return null

    const content = version.substring(0, 12)
    return (
        <MonoText {...props} style={{ ...style, fontFamily: 'Roboto Mono' }} tooltip={version}>
            {content}
        </MonoText>
    )
}

export { MonoText, Text, VersionText }
export default Text

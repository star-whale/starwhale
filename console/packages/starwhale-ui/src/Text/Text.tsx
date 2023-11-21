import React from 'react'
import { Tooltip } from '../Tooltip'
import { IconCopy } from '../Copy'
import { themedUseStyletron } from '../theme/styletron'
import TextLink from '../Link/TextLink'

export interface ITextProps {
    children?: React.ReactNode
    size?: 'small' | 'medium' | 'large' | 'xlarge'
    style?: React.CSSProperties
    tooltip?: React.ReactNode
    to?: string
    content?: string
    maxWidth?: string
}

const fontSizeMap: { [k in Exclude<ITextProps['size'], undefined>]: string } = {
    small: '12px',
    medium: '14px',
    large: '16px',
    xlarge: '20px',
}

function Text({ children, style, size = 'medium', tooltip = '', content = '', maxWidth = '200px', to }: ITextProps) {
    const [css] = themedUseStyletron()

    let Component = (
        <span
            style={style}
            className={css({
                fontSize: fontSizeMap[size],
                WebkitLineClamp: 1,
                display: '-webkit-box',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                verticalAlign: 'middle',
                WebkitBoxOrient: 'vertical',
                whiteSpace: 'normal',
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

    if (to) {
        Component = (
            <TextLink to={to} baseStyle={{ maxWidth: 'none' }}>
                {Component}
            </TextLink>
        )
    }

    return (
        <div className='flex items-center'>
            {Component}
            {content && <IconCopy content={content} />}
        </div>
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

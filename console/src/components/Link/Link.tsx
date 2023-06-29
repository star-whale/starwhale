import React from 'react'
import { StatefulTooltipProps } from 'baseui/tooltip'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { Link as BaseLink, LinkProps } from 'react-router-dom'
import Tooltip from '@starwhale/ui/Tooltip/Tooltip'

const useLinkStyles = createUseStyles({
    link: {
        'display': 'flex',
        'fontSize': '12px',
        'textDecoration': 'none',
        'color': 'gray',
        '&:hover': {
            color: ' #5181E0',
        },
    },
})

export type ILinkProps = {
    to: string
    tooltip?: StatefulTooltipProps | Pick<StatefulTooltipProps, 'content' | 'placement'>
    children: React.ReactNode
    style?: React.CSSProperties
    className?: string
} & LinkProps

export default function Link({ to, tooltip, className, style = {}, children, ...rest }: ILinkProps) {
    const styles = useLinkStyles()

    const { content, placement = 'top', ...tooltipRest } = tooltip || {}

    return (
        <Tooltip content={content} placement={placement} {...tooltipRest}>
            <BaseLink to={to} className={cn(className ?? styles.link)} style={style} {...rest}>
                {children}
            </BaseLink>
        </Tooltip>
    )
}

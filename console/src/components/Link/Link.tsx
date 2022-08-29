import React from 'react'
import { useStyletron } from 'baseui'
import { StatefulTooltip } from 'baseui/tooltip'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { Link as BaseLink } from 'react-router-dom'
import { StyleObject } from 'styletron-react'

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
    tooltip?: string
    children: React.ReactNode
    style?: React.CSSProperties
    className?: string
}

export default function Link({ to, tooltip, className, style = {}, children }: ILinkProps) {
    const [css] = useStyletron()
    const styles = useLinkStyles()

    return (
        <StatefulTooltip content={tooltip} placement='top'>
            <BaseLink to={to} className={cn(className ?? styles.link)} style={style}>
                {children}
            </BaseLink>
        </StatefulTooltip>
    )
}

import React from 'react'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import BaseLink, { ILinkProps } from './Link'

const useLinkStyles = createUseStyles({
    link: {
        textDecoration: 'none',
        flex: 1,
        width: '100%',
        maxWidth: '300px',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        display: 'inline-block',
        verticalAlign: 'middle',
    },
    text: {
        'display': 'initial',
        'fontSize': '14px',
        'color': '#2B65D9',
        '&:hover': {
            textDecoration: 'underline',
            color: ' #5181E0 ',
        },
        '&:hover span': {
            textDecoration: 'underline',
            color: ' #5181E0 ',
        },
        '&:visited': {
            color: '#1C4CAD ',
        },
    },
})

export default function TextLink({
    children,
    className,
    style,
    baseStyle,
    ...rest
}: ILinkProps & { baseStyle?: React.CSSProperties }) {
    const styles = useLinkStyles()

    return (
        <BaseLink className={styles.link} style={baseStyle} {...rest}>
            <div className={cn(styles.text, className)} style={style}>
                {children}
            </div>
        </BaseLink>
    )
}

import React from 'react'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import BaseLink, { ILinkProps } from './Link'

const useLinkStyles = createUseStyles({
    link: {
        // display: 'inline-block',
        textDecoration: 'none',
        flex: 1,
        width: '100%',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
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

export default function TextLink({ children, className, style, ...rest }: ILinkProps) {
    const styles = useLinkStyles()

    return (
        <BaseLink className={styles.link} {...rest}>
            <p className={cn(styles.text, className)} style={style}>
                {children}
            </p>
        </BaseLink>
    )
}

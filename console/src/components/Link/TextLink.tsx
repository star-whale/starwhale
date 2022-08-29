import React from 'react'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import BaseLink, { ILinkProps } from './Link'

const useLinkStyles = createUseStyles({
    link: {
        display: 'flex',
        textDecoration: 'none',
    },
    text: {
        'display': 'flex',
        'justifyContent': 'center',
        'alignItems': 'center',
        'fontSize': '14px',
        'fontWeight': 'bold',
        'color': '#02102B',
        '&:hover': {
            textDecoration: 'underline',
            color: ' #5181E0 ',
        },
    },
})

export default function TextLink({ children, className, ...rest }: ILinkProps) {
    const styles = useLinkStyles()

    return (
        <BaseLink {...rest}>
            <p className={cn(styles.text, className)}>{children}</p>
        </BaseLink>
    )
}

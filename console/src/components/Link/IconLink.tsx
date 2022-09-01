import React from 'react'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import BaseLink, { ILinkProps } from './Link'

const useLinkStyles = createUseStyles({
    link: {
        'display': 'flex',
        'fontSize': '12px',
        'backgroundColor': '#F4F5F7',
        'borderRadius': '2px',
        'width': '20px',
        'height': '20px',
        'textDecoration': 'none',
        'color': 'gray',
        '&:hover span': {
            color: ' #5181E0',
        },
        '&:hover': {
            color: ' #5181E0',
            backgroundColor: '#F0F4FF',
        },
    },
})

export default function IconLink({ children, className, ...rest }: ILinkProps) {
    const styles = useLinkStyles()

    return (
        <BaseLink className={cn('flex-row-center', styles.link, className)} {...rest}>
            {children}
        </BaseLink>
    )
}

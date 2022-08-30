import React from 'react'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import BaseLink, { ILinkProps } from './Link'
import { StyledLink } from 'baseui/link'

const useLinkStyles = createUseStyles({
    link: {
        'display': 'flex',
        'fontSize': '12px',
        'borderRadius': '2px',
        'textDecoration': 'none',
        'color': 'rgb(2, 16, 43)',
        '&:hover': {
            color: ' #5181E0',
            textDecoration: 'underline',
        },
    },
})

export type IButtonLinkProps = {
    tooltip?: string
    children: React.ReactNode
    style?: React.CSSProperties
    className?: string
    onClick?: () => void
}

export default function ButtonLink({ children, className, onClick, ...rest }: IButtonLinkProps) {
    const styles = useLinkStyles()

    return (
        <StyledLink animateUnderline={false} className={cn(styles.link)} onClick={onClick} {...rest}>
            {children}
        </StyledLink>
    )
}
// 'row-center--inline', 'gap4',

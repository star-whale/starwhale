import React from 'react'
import cn from 'classnames'
import { LinkProps, StyledLink } from 'baseui/link'

export type IButtonLinkProps = {
    tooltip?: string
    children: React.ReactNode
    style?: React.CSSProperties
    className?: string
    onClick?: () => void
} & LinkProps

export default function ButtonLink({ children, className, onClick, ...rest }: IButtonLinkProps) {
    return (
        <StyledLink
            animateUnderline={false}
            className={cn('!button-link !rounded-4px', className)}
            onClick={onClick}
            {...rest}
        >
            {children}
        </StyledLink>
    )
}

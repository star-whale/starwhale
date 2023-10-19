import React from 'react'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { LinkProps, StyledLink } from 'baseui/link'
import { expandPadding } from '../../../packages/starwhale-ui/src/utils/index'

const useLinkStyles = createUseStyles({
    link: {
        'display': 'flex',
        'fontSize': '12px',
        'borderRadius': '4px',
        'textDecoration': 'none',
        'color': 'rgb(2, 16, 43)',
        'borderWidth': '1px',
        'borderColor': '#2B65D9',
        ...expandPadding('9px', '12px', '9px', '12px'),
        'lineHeight': '1',
        '&:hover': {
            color: ' #5181E0',
            borderColor: '#5181E0',
        },
    },
})

export type IButtonLinkProps = {
    tooltip?: string
    children: React.ReactNode
    style?: React.CSSProperties
    className?: string
    onClick?: () => void
} & LinkProps

export default function ButtonLink({ children, className, onClick, ...rest }: IButtonLinkProps) {
    const styles = useLinkStyles()

    return (
        <StyledLink animateUnderline={false} className={cn(styles.link, className)} onClick={onClick} {...rest}>
            {children}
        </StyledLink>
    )
}
// 'row-center--inline', 'gap4',

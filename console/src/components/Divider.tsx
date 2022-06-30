import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import { IThemedStyleProps } from '@/theme'
import { useStyletron } from 'baseui'
import classNames from 'classnames'
import React from 'react'
import { createUseStyles } from 'react-jss'

const useStyles = createUseStyles({
    wrapper: (props: IThemedStyleProps) => {
        return {
            'fontSize': '14px',
            'display': 'flex',
            'alignItems': 'center',
            'justifyContent': 'center',
            '&:before': {
                content: '""',
                position: 'relative',
                top: '50%',
                borderTop: `1px solid ${props.theme.borders.border300.borderColor}`,
                transform: 'translateY(50%)',
            },
            '&:after': {
                content: '""',
                position: 'relative',
                top: '50%',
                borderTop: `1px solid ${props.theme.borders.border300.borderColor}`,
                transform: 'translateY(50%)',
            },
        }
    },
    center: {
        'margin': '10px 0',

        '&:before': {
            width: '50%',
        },
        '&:after': {
            width: '50%',
        },
    },
    left: {
        'margin': '10px 0',

        '&:before': {
            width: '0%',
        },
        '&:after': {
            width: '100%',
        },
        '& $innerText': {
            paddingLeft: 0,
        },
    },
    right: {
        'margin': '10px 0',

        '&:before': {
            width: '100%',
        },
        '&:after': {
            width: '0%',
        },
        '& $innerText': {
            paddingRight: 0,
        },
    },
    top: {
        'position': 'relative',
        'marginTop': '30px',
        'marginBottom': '20px',
        '&:before': {
            width: '0%',
        },
        '&:after': {
            width: '100%',
            marginLeft: 0,
        },
        '& $innerText': {
            position: 'absolute',
            top: '-24px',
            left: 0,
            paddingLeft: '0px',
        },
    },
    innerText: {
        flexShrink: 0,
        fontWeight: '500',
        padding: '0 1em',
        color: 'var(--color-brandFontNote) !important',
    },
})

export interface IDividerProps {
    children: React.ReactNode
    orientation?: 'left' | 'center' | 'right' | 'top'
}

export default function Divider({ children, orientation = 'center' }: IDividerProps) {
    const themeType = useCurrentThemeType()
    const [, theme] = useStyletron()
    const styles = useStyles({ themeType, theme })
    return (
        <div className={classNames(styles.wrapper, styles[orientation])}>
            <div className={styles.innerText}>{children}</div>
        </div>
    )
}

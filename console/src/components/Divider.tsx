import { IThemedStyleProps } from '@starwhale/ui/theme'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import classNames from 'classnames'
import React from 'react'
import { createUseStyles } from 'react-jss'

const useStyles = createUseStyles({
    divider: (props: IThemedStyleProps) => {
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
        'marginTop': '40px',
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
            top: '-25px',
            left: 0,
            paddingLeft: '0px',
        },
    },
    innerText: (props: IThemedStyleProps) => ({
        flexShrink: 0,
        fontWeight: '600',
        padding: '0 1em',
        color: `${props.theme.brandFontNote}`,
    }),
})

export interface IDividerProps {
    children: React.ReactNode
    orientation?: 'left' | 'center' | 'right' | 'top'
}

export default function Divider({ children, orientation = 'center' }: IDividerProps) {
    const [, theme] = themedUseStyletron()
    const styles = useStyles({ theme })
    return (
        <div className={classNames(styles.divider, styles[orientation])}>
            <div className={styles.innerText}>{children}</div>
        </div>
    )
}

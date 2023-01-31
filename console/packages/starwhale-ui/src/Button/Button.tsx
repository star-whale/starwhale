import React from 'react'
import { Button as BaseButton, ButtonProps, KIND } from 'baseui/button'
import { mergeOverrides } from '../utils'
import { themedUseStyletron } from '../theme/styletron'

export interface IButtonProps extends ButtonProps {
    as?: 'link' | 'button' | 'transparent' | 'withIcon'
    kind?: keyof typeof KIND
    isFull?: boolean
    className?: string
}

export default function Button({
    isFull = false,
    size = 'compact',
    kind = 'primary',
    as = 'button',
    children,
    ...props
}: IButtonProps) {
    const [, theme] = themedUseStyletron()
    const defaultStyles: React.CSSProperties = {
        borderTopLeftRadius: theme.borders.radius200,
        borderTopRightRadius: theme.borders.radius200,
        borderBottomLeftRadius: theme.borders.radius200,
        borderBottomRightRadius: theme.borders.radius200,
        lineHeight: '14px',
        paddingTop: '9px',
        paddingBottom: '9px',
        paddingLeft: '9px',
        paddingRight: '9px',
    }

    let overrides = mergeOverrides(
        {
            BaseButton: {
                style: {
                    ...defaultStyles,
                    width: isFull ? '100%' : 'auto',
                },
            },
        },
        props.overrides
    )

    if (as === 'link') {
        overrides = mergeOverrides(
            {
                BaseButton: {
                    style: {
                        ...defaultStyles,
                        'lineHeight': '14px',
                        'paddingLeft': '0',
                        'paddingRight': '0',
                        'paddingBottom': '0',
                        'paddingTop': '0',
                        'marginBottom': '0',
                        'marginTop': '0',
                        'marginLeft': '0',
                        'marginRight': '0',
                        'backgroundColor': 'transparent',
                        'color': '#2B65D9',
                        ':hover': {
                            backgroundColor: 'transparent',
                        },
                    },
                },
            },
            props.overrides
        )
    } else if (as === 'transparent') {
        overrides = mergeOverrides(
            {
                BaseButton: {
                    style: {
                        ...defaultStyles,
                        'backgroundColor': 'transparent',
                        'color': 'rgba(2,16,43,0.20)',
                        ':hover': {
                            backgroundColor: 'transparent',
                            color: theme.brandPrimaryHover,
                        },
                        ':focus': {
                            backgroundColor: 'transparent',
                            color: theme.brandPrimaryHover,
                        },
                    },
                },
            },
            props.overrides
        )
    } else if (as === 'withIcon') {
        overrides = mergeOverrides(
            {
                BaseButton: {
                    style: {
                        ...defaultStyles,
                        'backgroundColor': '#F4F5F7',
                        'color': 'rgba(2,16,43,0.60)',
                        ':hover': {
                            backgroundColor: '#F0F4FF',
                            color: '#5181E0',
                        },
                        ':active': {
                            backgroundColor: '#F0F4FF',
                            color: '#1C4CAD',
                        },
                    },
                },
            },
            props.overrides
        )
    }

    return (
        <BaseButton size={size} kind={kind} {...props} overrides={overrides}>
            {children}
        </BaseButton>
    )
}

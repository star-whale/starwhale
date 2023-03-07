import React from 'react'
import { Button as BaseButton, ButtonProps, KIND } from 'baseui/button'
import { mergeOverrides } from '../utils'
import { themedUseStyletron } from '../theme/styletron'
import IconFont, { IconTypesT } from '../IconFont'

export interface IButtonProps extends ButtonProps {
    as?: 'link' | 'transparent' | 'negative'
    kind?: keyof typeof KIND
    isFull?: boolean
    className?: string
    icon?: IconTypesT
}

function Button(
    { isFull = false, size = 'compact', kind = 'primary', as, icon, children, ...props }: IButtonProps,
    ref: React.Ref<HTMLButtonElement>
) {
    const [, theme] = themedUseStyletron()
    const defaultStyles: React.CSSProperties = {
        borderTopLeftRadius: theme.borders.radius200,
        borderTopRightRadius: theme.borders.radius200,
        borderBottomLeftRadius: theme.borders.radius200,
        borderBottomRightRadius: theme.borders.radius200,
        lineHeight: '14px',
        paddingTop: kind === 'tertiary' ? '5px' : '9px',
        paddingBottom: kind === 'tertiary' ? '5px' : '9px',
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
            StartEnhancer: {
                style: {
                    marginRight: icon ? '0' : '5px',
                },
            },
        },
        props.overrides
    )

    if (icon && !props.startEnhancer) {
        // eslint-disable-next-line no-param-reassign
        props.startEnhancer = () => <IconFont type={icon} size={13} />
    }

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
                        'color': theme.colors.buttonPrimaryFill,
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
                            color: 'rgba(2,16,43,0.20)',
                        },
                    },
                },
            },
            props.overrides
        )
    } else if (as === 'negative') {
        overrides = mergeOverrides(
            {
                BaseButton: {
                    style: {
                        ...defaultStyles,
                        'backgroundColor': '#FFEBEB',
                        'color': ' #CC3D3D',
                        ':hover': {
                            backgroundColor: '#FFDBDB',
                        },
                        ':focus': {
                            backgroundColor: '#FFCCCC',
                        },
                    },
                },
            },
            props.overrides
        )
    }

    return (
        <BaseButton size={size} kind={kind} {...props} overrides={overrides} ref={ref}>
            {children}
        </BaseButton>
    )
}

const ForwardButton = React.forwardRef<HTMLButtonElement, IButtonProps>(Button)
ForwardButton.displayName = 'Button'

export default ForwardButton

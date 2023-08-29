import React from 'react'
import { Button as BaseButton, ButtonProps, KIND } from 'baseui/button'
import { mergeOverrides } from '../utils'
import { themedUseStyletron } from '../theme/styletron'
import IconFont, { IconTypesT } from '../IconFont'
import { Tooltip } from '../Tooltip'

export interface IButtonProps extends ButtonProps {
    as?: 'link' | 'transparent' | 'negative'
    kind?: keyof typeof KIND
    isFull?: boolean
    className?: string
    icon?: IconTypesT
}

export interface IExtendButtonProps extends IButtonProps {
    noPadding?: boolean
    negative?: boolean
    tooltip?: string
    transparent?: boolean
    iconDisable?: boolean
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

    let overrides: any = null

    if (icon && !props.startEnhancer) {
        // eslint-disable-next-line no-param-reassign
        props.startEnhancer = () => <IconFont type={icon} size={16} />
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
                            color: '#5181E0',
                            backgroundColor: 'transparent',
                        },
                        ':focus': {
                            color: '#5181E0',
                            backgroundColor: 'transparent',
                        },
                        ':active': {
                            color: '#5181E0',
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
                        ':active': {
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
                        ':active': {
                            backgroundColor: '#FFCCCC',
                        },
                    },
                },
            },
            props.overrides
        )
    } else {
        overrides = mergeOverrides(
            {
                BaseButton: {
                    style: {
                        ...defaultStyles,
                    },
                },
            },
            props.overrides
        )
    }

    overrides = mergeOverrides(
        {
            BaseButton: {
                style: {
                    width: isFull ? '100%' : 'auto',
                },
            },
            StartEnhancer: {
                style: {
                    marginRight: !children ? '0' : '5px',
                },
            },
        },
        overrides
    )

    return (
        <BaseButton size={size} kind={kind} {...props} overrides={overrides} ref={ref}>
            {children}
        </BaseButton>
    )
}

const ForwardButton = React.forwardRef<HTMLButtonElement, IButtonProps>(Button as any)
ForwardButton.displayName = 'Button'
ForwardButton.defaultProps = {
    kind: 'primary',
    as: undefined,
    isFull: false,
    icon: undefined,
    className: undefined,
}

const ExtendButton = React.forwardRef<HTMLButtonElement, IExtendButtonProps>((props, ref: any) => {
    const [, theme] = themedUseStyletron()
    const STYLES = {
        noPadding: {
            BaseButton: {
                style: {
                    lineHeight: '1',
                    paddingLeft: '0',
                    paddingRight: '0',
                    paddingBottom: '0',
                    paddingTop: '0',
                    marginBottom: '0',
                    marginTop: '0',
                    marginLeft: '0',
                    marginRight: '0',
                    alignSelf: 'center',
                },
            },
        },
        transparent: {
            BaseButton: {
                style: {
                    'backgroundColor': 'transparent',
                    'color': theme.colors.buttonPrimaryFill,
                    ':hover': {
                        backgroundColor: 'transparent',
                    },
                    ':active': {
                        backgroundColor: 'transparent',
                    },
                    ':focus': {
                        backgroundColor: 'transparent',
                    },
                },
            },
        },
        negative: {
            BaseButton: {
                style: {
                    'color': '#CC3D3D',
                    'backgroundColor': 'transparent',
                    ':hover': {
                        color: '#D65E5E',
                        backgroundColor: 'transparent',
                    },
                    ':active': {
                        color: ' #A32727',
                        backgroundColor: 'transparent',
                    },
                    ':focus': {
                        color: ' #A32727',
                        backgroundColor: 'transparent',
                    },
                },
            },
        },
        // btn & icon & disabled
        iconDisable: {
            BaseButton: {
                style: {
                    'color': 'rgba(2,16,43,0.40) !important',
                    ':disabled': {
                        backgroundColor: 'transparent',
                    },
                    ':hover': {
                        color: 'rgba(2,16,43,0.40);',
                        backgroundColor: 'transparent',
                    },
                },
            },
        },
    }
    const overrides = [
        props.noPadding ? STYLES.noPadding : {},
        props.transparent ? STYLES.transparent : {},
        props.negative ? STYLES.negative : {},
        props.iconDisable ? STYLES.iconDisable : {},
        props.overrides ? props.overrides : {},
    ].reduce(mergeOverrides, {})

    if (props.tooltip) {
        return (
            <Tooltip content={props.tooltip} showArrow placement='top'>
                <div>
                    <ForwardButton {...props} overrides={overrides} ref={ref} />
                </div>
            </Tooltip>
        )
    }

    return <ForwardButton {...props} overrides={overrides} ref={ref} />
})
ExtendButton.displayName = 'ExtendButton'
ExtendButton.defaultProps = {
    noPadding: false,
    transparent: false,
    negative: false,
    kind: 'primary',
    as: undefined,
    isFull: false,
    icon: undefined,
    className: undefined,
    tooltip: '',
    iconDisable: false,
}

export { ExtendButton }

export default ForwardButton

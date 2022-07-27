import React from 'react'
import { Button as BaseButton, ButtonProps, KIND } from 'baseui/button'
import { mergeOverrides } from '@/utils/baseui'
import { useStyletron } from 'baseui'

export interface IButtonProps extends ButtonProps {
    as?: 'link' | 'button' | 'transparent'
    kind?: KIND[keyof KIND]
    isFull?: boolean
    className?: string
}

// const useStyles = createUseStyles({
// baseButton: {
//     style: {
//         borderRadius: theme.borders.radius100,
//     }
// }
// })

/* eslint-disable react/jsx-props-no-spreading */
export default function Button({
    isFull = false,
    size = 'compact',
    kind = 'primary',
    as = 'button',
    children,
    ...props
}: IButtonProps) {
    // const styles = useStyles()
    const [, theme] = useStyletron()

    let overrides = mergeOverrides(
        {
            BaseButton: {
                style: {
                    borderTopLeftRadius: theme.borders.radius200,
                    borderTopRightRadius: theme.borders.radius200,
                    borderBottomLeftRadius: theme.borders.radius200,
                    borderBottomRightRadius: theme.borders.radius200,
                    lineHeight: '14px',
                    paddingTop: '9px',
                    paddingBottom: '9px',
                    paddingLeft: '9px',
                    paddingRight: '9px',
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
                        'borderTopLeftRadius': theme.borders.radius200,
                        'borderTopRightRadius': theme.borders.radius200,
                        'borderBottomLeftRadius': theme.borders.radius200,
                        'borderBottomRightRadius': theme.borders.radius200,
                        'lineHeight': '14px',
                        'width': isFull ? '100%' : 'auto',
                        // 'paddingBottom': '0',
                        // 'paddingTop': '0',
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
                        'borderTopLeftRadius': theme.borders.radius200,
                        'borderTopRightRadius': theme.borders.radius200,
                        'borderBottomLeftRadius': theme.borders.radius200,
                        'borderBottomRightRadius': theme.borders.radius200,
                        'width': isFull ? '100%' : 'auto',
                        'backgroundColor': 'transparent',
                        'color': 'rgba(2,16,43,0.20)',
                        ':hover': {
                            backgroundColor: 'transparent',
                            color: 'var(--color-brandPrimaryHover)',
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

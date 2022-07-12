import React from 'react'
import { createUseStyles } from 'react-jss'
import { Button as BaseButton, ButtonProps, KIND } from 'baseui/button'
import classNames from 'classnames'
import { mergeOverrides } from '@/utils/baseui'

export interface IButtonProps extends ButtonProps {
    as?: 'link' | 'button' | 'transparent'
    kind?: KIND[keyof KIND]
    isFull?: boolean
    className?: string
}

const useStyles = createUseStyles({
    baseButton: {
        borderRadius: '4px',
    },
})

/* eslint-disable react/jsx-props-no-spreading */
export default function Button({
    isFull = false,
    size = 'compact',
    kind = 'primary',
    as = 'button',
    children,
    ...props
}: IButtonProps) {
    const styles = useStyles()

    let overrides = mergeOverrides(
        {
            BaseButton: {
                style: {
                    borderRadius: '4px',
                    lineHeight: '14px',
                    padding: '9px',
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
                        'borderRadius': '4px',
                        'lineHeight': '14px',
                        'width': isFull ? '100%' : 'auto',
                        'padding': '0',
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
                        'borderRadius': '4px',
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
        <BaseButton size={size} kind={kind} className={classNames(styles.baseButton)} {...props} overrides={overrides}>
            {children}
        </BaseButton>
    )
}

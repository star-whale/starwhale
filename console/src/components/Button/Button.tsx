import React from 'react'
import { createUseStyles } from 'react-jss'
import { Button as BaseButton, ButtonProps, KIND } from 'baseui/button'
import classNames from 'classnames'
import { mergeOverrides } from '@/utils/baseui'

export interface IButtonProps extends ButtonProps {
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
    children,
    ...props
}: IButtonProps) {
    const styles = useStyles()

    const overrides = mergeOverrides(
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

    return (
        <BaseButton size={size} kind={kind} className={classNames(styles.baseButton)} {...props} overrides={overrides}>
            {children}
        </BaseButton>
    )
}

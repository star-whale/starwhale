import React from 'react'
import { createUseStyles } from 'react-jss'
import { Button as BaseButton, ButtonProps, KIND } from 'baseui/button'
import classNames from 'classnames'

export interface IButtonProps extends Omit<ButtonProps, 'kind'> {
    kind?: KIND[keyof KIND] | 'full'
    className?: string
}

const useStyles = createUseStyles({
    baseButton: {
        borderRadius: '4px',
        color: '#fff',
    },
    full: {
        flexGrow: 1,
    },
    primary: {},
    secondary: {},
    tertiary: {},
})

/* eslint-disable react/jsx-props-no-spreading */
export default function Button({ kind = 'primary', children, ...props }: IButtonProps) {
    const styles = useStyles()

    return (
        <BaseButton className={classNames(styles.baseButton, styles[kind])} {...props}>
            {children}
        </BaseButton>
    )
}

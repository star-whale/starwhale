import React from 'react'
import { createUseStyles } from 'react-jss'
import { Button as BaseButton, ButtonProps } from 'baseui/button'

export interface IButtonProps extends ButtonProps {
    className?: string
}

const useStyles = createUseStyles({
    baseButton: {
        borderRadius: '4px',
        color: '#fff',
    },
})

/* eslint-disable react/jsx-props-no-spreading */
export default function Button({ children, ...props }: IButtonProps) {
    const styles = useStyles()

    return (
        <BaseButton className={styles.baseButton} {...props}>
            {children}
        </BaseButton>
    )
}

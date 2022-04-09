import React, { useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { createUseStyles } from 'react-jss'
import { useStyletron } from 'baseui'
import classNames from 'classnames'
import { BiMoon, BiSun } from 'react-icons/bi'
import { IThemedStyleProps } from '@/interfaces/IThemedStyle'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import { useThemeType } from '@/hooks/useThemeType'

const useThemeToggleStyles = createUseStyles({
    root: ({ theme }: IThemedStyleProps) => ({
        position: 'relative',
        cursor: 'pointer',
        border: `1px solid ${theme.borders.border300.borderColor}`,
        borderRadius: 18,
        height: 18,
    }),
    track: () => ({
        height: 18,
        padding: '0 4px',
        transition: 'all 0.2s ease',
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center',
    }),
    thumb: ({ theme }: IThemedStyleProps) => ({
        position: 'absolute',
        height: 18,
        width: 18,
        padding: 1,
        top: -1,
        left: -2,
        borderRadius: '50%',
        background: theme.colors.contentPrimary,
        color: theme.colors.backgroundPrimary,
        transition: 'all 0.5s cubic-bezier(0.23, 1, 0.32, 1) 0ms',
        transform: 'translateX(0)',
        textAlign: 'center',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    }),
    checked: () => ({
        transform: 'translateX(24px)',
    }),
})

export interface IThemeToggleProps {
    className?: string
}

export default function ThemeToggle({ className }: IThemeToggleProps) {
    const [, theme] = useStyletron()
    const themeType = useCurrentThemeType()
    const styles = useThemeToggleStyles({ theme, themeType })
    const { setThemeType } = useThemeType()
    const checked = themeType === 'dark'

    return (
        <div
            role='button'
            tabIndex={0}
            className={classNames(className, styles.root)}
            onClick={() => {
                const newThemeType = themeType === 'dark' ? 'light' : 'dark'
                setThemeType(newThemeType)
            }}
        >
            <div className={styles.track}>
                <BiSun />
                <BiMoon style={{ marginLeft: 4 }} />
            </div>
            <div className={classNames({ [styles.thumb]: true, [styles.checked]: checked })}>
                {!checked ? <BiSun /> : <BiMoon />}
            </div>
        </div>
    )
}

import React from 'react'
import { Link } from 'react-router-dom'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import smallLogoImg from '@/assets/logo_small_en_white.svg'
import normalLogoImg from '@/assets/logo_normal_en_white.svg'
import normalLogoGrayImg from '@/assets/logo_normal_en_gray.svg'
import { IComposedComponentProps } from '@/theme'
import { createUseStyles } from 'react-jss'
import classNames from 'classnames'
import { sidebarFoldedWidth, sidebarExpandedWidth, headerHeight } from '@/consts'

const useLogoStyles = createUseStyles({
    logoWrapper: {
        display: 'flex',
        flexDirection: 'row',
        textDecoration: 'none',
        alignItems: 'center',
        justifyContent: 'center',
        transition: 'width 100ms cubic-bezier(0.7, 0.1, 0.33, 1) 0ms',
        height: headerHeight,
    },
})

const normals = {
    white: normalLogoImg,
    gray: normalLogoGrayImg,
}

const smalls = {
    white: smallLogoImg,
    gray: smallLogoImg,
}

export interface ILogoProps extends IComposedComponentProps {
    expanded?: boolean
    kind?: 'gray' | 'white'
}

export default function Logo({ expanded = true, className, kind = 'white', style }: ILogoProps) {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    const styles = useLogoStyles()
    const logo = React.useMemo(() => {
        return (
            <>
                <img
                    style={{
                        width: 128,
                        position: 'relative',
                        left: '-10px',
                        display: expanded ? 'inline' : 'none',
                    }}
                    src={normals[kind]}
                    alt='logo'
                />
                <img
                    style={{
                        width: 50,
                        display: !expanded ? 'inline' : 'none',
                    }}
                    src={smalls[kind]}
                    alt='logo'
                />
            </>
        )
    }, [expanded, kind])

    if (!currentUser) {
        return (
            <div
                className={classNames(styles.logoWrapper, className)}
                style={{
                    width: expanded ? sidebarExpandedWidth : sidebarFoldedWidth,
                    transition: 'all .2s ease',
                    ...style,
                }}
            >
                {logo}
            </div>
        )
    }

    return (
        <Link
            className={classNames(styles.logoWrapper, className)}
            style={{
                width: expanded ? sidebarExpandedWidth : sidebarFoldedWidth,
                transition: 'all .2s ease',
                ...style,
            }}
            to='/'
        >
            {logo}
        </Link>
    )
}

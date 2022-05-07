import React, { useContext, useEffect, useRef, useState } from 'react'
import { fetchCurrentUser } from '@user/services/user'
import { useQuery } from 'react-query'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import axios from 'axios'
import { toaster } from 'baseui/toast'
import { getErrMsg, setToken } from '@/api'
import qs from 'qs'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { useLocation } from 'react-router-dom'
import { useStyletron } from 'baseui'
import { headerHeight } from '@/consts'
import { SidebarContext } from '@/contexts/SidebarContext'
import useTranslation from '@/hooks/useTranslation'
import { createUseStyles } from 'react-jss'
import { IThemedStyleProps } from '@/theme'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import User from '@/domain/user/components/User'
import { simulationJump } from '@/utils'
import { FiLogOut } from 'react-icons/fi'
import HeaderLeftMenu from './HeaderLeftMenu'
import Logo from './Logo'

const useHeaderStyles = createUseStyles({
    headerWrapper: {
        padding: '0 32px 0 0',
        position: 'fixed',
        background: 'var(--color-brandHeaderBackground)',
        backdropFilter: 'blur(10px)',
        zIndex: 1000,
        top: 0,
        height: `${headerHeight}px`,
        width: '100%',
        display: 'flex',
        flexFlow: 'row nowrap',
        alignItems: 'center',
        color: 'var(--color-contentPrimary)',
    },
})

const useStyles = createUseStyles({
    userWrapper: {
        'position': 'relative',
        'cursor': 'pointer',
        'display': 'flex',
        'align-items': 'center',
        'min-width': '140px',
        'height': '100%',
        'margin-left': '20px',
        'flex-direction': 'column',
        '&:hover': {
            '& $userMenu': {
                display: 'flex',
            },
        },
    },
    userNameWrapper: {
        'height': '100%',
        'display': 'flex',
        'color': 'var(--color-brandBackground4)',
        'align-items': 'center',
    },
    userMenu: (props: IThemedStyleProps) => ({
        'position': 'absolute',
        'top': '100%',
        'display': 'none',
        'margin': 0,
        'padding': 0,
        'line-height': 1.6,
        'flex-direction': 'column',
        'width': '100%',
        'font-size': '13px',
        'box-shadow': props.theme.lighting.shadow400,
        '& a': {
            '&:link': {
                'color': props.theme.colors.contentPrimary,
                'text-decoration': 'none',
            },
            '&:hover': {
                'color': props.theme.colors.contentPrimary,
                'text-decoration': 'none',
            },
            '&:visited': {
                'color': props.theme.colors.contentPrimary,
                'text-decoration': 'none',
            },
        },
    }),
    userMenuItem: (props: IThemedStyleProps) => ({
        'padding': '8px 12px',
        'display': 'flex',
        'align-items': 'center',
        'gap': '10px',
        'color': props.theme.colors.contentPrimary,
        '&:hover': {
            // background: color(props.theme.colors.background)
            //     .darken(props.themeType === 'light' ? 0.06 : 0.2)
            //     .rgb()
            //     .string(),
        },
    }),
})

export default function Header() {
    const [, theme] = useStyletron()
    const themeType = useCurrentThemeType()
    const styles = useStyles({ theme, themeType })
    const headerStyles = useHeaderStyles({ theme })
    const location = useLocation()
    const errMsgExpireTimeSeconds = 5
    const lastErrMsgRef = useRef<Record<string, number>>({})
    const lastLocationPathRef = useRef(location.pathname)
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser, setCurrentUser } = useCurrentUser()
    const userInfo = useQuery('currentUser', fetchCurrentUser, { enabled: false })

    // TODO:  refact move to sep file
    useEffect(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        if ((axios.interceptors.response as any).handlers.length > 0) {
            return
        }
        axios.interceptors.response.use(
            (response) => {
                if (response.headers.authorization) setToken(response.headers.authorization)
                return response.data?.data ? response.data : response
            },
            (error) => {
                const errMsg = getErrMsg(error)
                if (error.response?.status === 403 && error.config.method === 'get') {
                    const search = qs.parse(location.search, { ignoreQueryPrefix: true })
                    let { redirect } = search
                    if (redirect && typeof redirect === 'string') {
                        redirect = decodeURI(redirect)
                    } else if (['/login', '/logout'].indexOf(location.pathname) < 0) {
                        redirect = `${location.pathname}${location.search}`
                    } else {
                        redirect = '/'
                    }
                    if (location.pathname !== '/login' && location.pathname !== '/login/') {
                        window.location.href = `${window.location.protocol}//${
                            window.location.host
                        }/login?redirect=${encodeURIComponent(redirect)}`
                    }
                } else if (Date.now() - (lastErrMsgRef.current[errMsg] || 0) > errMsgExpireTimeSeconds * 1000) {
                    toaster.negative(errMsg, { autoHideDuration: (errMsgExpireTimeSeconds + 1) * 1000 })
                    lastErrMsgRef.current[errMsg] = Date.now()
                }
                return Promise.reject(error)
            }
        )
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useEffect(() => {
        if (userInfo.isSuccess) {
            setCurrentUser(userInfo.data)
        }
    }, [userInfo.data, userInfo.isSuccess, setCurrentUser])

    useEffect(() => {
        if (location.pathname !== '/login' && location.pathname !== '/login/' && !currentUser) {
            userInfo.refetch()
        }
    }, [userInfo, location.pathname, currentUser])

    useEffect(() => {
        if (lastLocationPathRef.current !== location.pathname) {
            lastErrMsgRef.current = {}
        }
        lastLocationPathRef.current = location.pathname
    }, [location.pathname])

    const ctx = useContext(SidebarContext)
    const [t] = useTranslation()

    const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false)
    // const handleChangePassword = useCallback(
    //     async (data: IChangePasswordSchema) => {
    //         await changePassword(data)
    //         setIsChangePasswordOpen(false)
    //         toaster.positive(t('Password Changed'), { autoHideDuration: 2000 })
    //     },
    //     [t]
    // )

    return (
        <header className={headerStyles.headerWrapper}>
            <Logo expanded={ctx.expanded} />
            <div>{currentUser && <HeaderLeftMenu />}</div>
            <div style={{ flexGrow: 1 }} />
            {currentUser && (
                <div className={styles.userWrapper}>
                    <div className={styles.userNameWrapper}>
                        <User user={currentUser} />
                    </div>
                    <div className={styles.userMenu}>
                        {/* <div
                            role='button'
                            tabIndex={0}
                            className={styles.userMenuItem}
                            onClick={() => {
                                setIsChangePasswordOpen(true)
                            }}
                        >
                            <MdPassword size={12} />
                            <span>{t('password')}</span>
                        </div> */}
                        <div
                            role='button'
                            tabIndex={0}
                            className={styles.userMenuItem}
                            onClick={() => {
                                setToken(undefined)
                                simulationJump('/logout')
                            }}
                        >
                            <FiLogOut size={12} />
                            <span>{t('Logout')}</span>
                        </div>
                    </div>
                </div>
            )}
            <Modal
                isOpen={isChangePasswordOpen}
                onClose={() => setIsChangePasswordOpen(false)}
                closeable
                animate
                autoFocus
            >
                <ModalHeader>{t('Change Password')}</ModalHeader>
                <ModalBody>{/* <PasswordForm onSubmit={handleChangePassword} /> */}</ModalBody>
            </Modal>
        </header>
    )
}

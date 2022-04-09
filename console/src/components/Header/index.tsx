import React, { useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { changePassword, fetchCurrentUser } from '@user/services/user'
import { useQuery } from 'react-query'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import axios from 'axios'
import { toaster } from 'baseui/toast'
import { getErrMsg } from '@/utils/error'
import qs from 'qs'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { Link, useLocation, useParams } from 'react-router-dom'
import { useStyletron } from 'baseui'
import { headerHeight, resourceIconMapping } from '@/consts'
import { SidebarContext } from '@/contexts/SidebarContext'
import useTranslation from '@/hooks/useTranslation'
import { BiMoon, BiSun } from 'react-icons/bi'
import color from 'color'
import { createUseStyles } from 'react-jss'
import { IThemedStyleProps } from '@/interfaces/IThemedStyle'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import classNames from 'classnames'
import User from '@/components/User'
import Text from '@/components/Text'
import { IChangePasswordSchema } from '@/domain/user/schemas/user'
import i18n from '@/i18n'
import { simulationJump } from '@/utils'
import { colors } from '@/consts/theme'
import { FiLogOut } from 'react-icons/fi'
import { MdPassword } from 'react-icons/md'
import PasswordForm from '@/components/PasswordForm'
import ThemeToggle from './ThemeToggle'
import HeaderLeftMenu from './HeaderLeftMenu'
import { useProject, useProjectLoading } from '@/domain/project/hooks/useProject'
import { fetchProject } from '@/domain/project/services/project'
import { useToken } from '../../hooks/useToken'

const useHeaderStyles = createUseStyles({
    headerWrapper: (props: IThemedStyleProps) => ({
        padding: '0 32px 0 0',
        position: 'fixed',
        background: color(props.themeType === 'light' ? colors.brand2 : props.theme.colors.backgroundPrimary)
            .fade(props.themeType === 'light' ? 0 : 0.5)
            .string(),
        borderBottom: `1px solid ${props.theme.borders.border300.borderColor}`,
        backdropFilter: 'blur(10px)',
        zIndex: 1000,
        top: 0,
        height: `${headerHeight}px`,
        width: '100%',
        display: 'flex',
        flexFlow: 'row nowrap',
        boxSizing: 'border-box',
        alignItems: 'center',
        color: '#FFF',
    }),
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
    userAvatarWrapper: {
        'height': '100%',
        'display': 'flex',
        'align-items': 'center',
    },
    userMenu: (props: IThemedStyleProps) => ({
        'background': props.theme.colors.background,
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
            background: color(props.theme.colors.background)
                .darken(props.themeType === 'light' ? 0.06 : 0.2)
                .rgb()
                .string(),
        },
    }),
})

export default function Header() {
    const [css, theme] = useStyletron()
    const themeType = useCurrentThemeType()
    const styles = useStyles({ theme, themeType })
    const headerStyles = useHeaderStyles({ theme, themeType })
    const location = useLocation()
    const { token, setToken } = useToken()
    const errMsgExpireTimeSeconds = 5
    const lastErrMsgRef = useRef<Record<string, number>>({})
    const lastLocationPathRef = useRef(location.pathname)

    useEffect(() => {
        if (lastLocationPathRef.current !== location.pathname) {
            lastErrMsgRef.current = {}
        }
        lastLocationPathRef.current = location.pathname
    }, [location.pathname])

    // todo refact move to sep file
    useEffect(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        if ((axios.interceptors.response as any).handlers.length > 0) {
            return
        }
        // todo refact
        axios.interceptors.response.use(
            (response) => {
                console.log(response)
                // return response.data
                response.headers.authorization && setToken(response.headers.authorization)
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

    const { currentUser, setCurrentUser } = useCurrentUser()
    const userInfo = useQuery('currentUser', fetchCurrentUser)
    useEffect(() => {
        if (userInfo.isSuccess) {
            setCurrentUser(userInfo.data)
        }
    }, [userInfo.data, userInfo.isSuccess, setCurrentUser])

    const ctx = useContext(SidebarContext)
    const [t] = useTranslation()

    const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false)
    const handleChangePassword = useCallback(
        async (data: IChangePasswordSchema) => {
            await changePassword(data)
            setIsChangePasswordOpen(false)
            toaster.positive(t('password changed'), { autoHideDuration: 2000 })
        },
        [t]
    )

    // const currentThemeType = useCurrentThemeType()
    if (!!!currentUser) {
        return <></>
    }

    return (
        <header className={headerStyles.headerWrapper}>
            <Link
                style={{
                    flex: '0 0 200px',
                    display: 'flex',
                    flexDirection: 'row',
                    textDecoration: 'none',
                    alignItems: 'center',
                    justifyContent: 'center',
                    boxSizing: 'border-box',
                    transition: 'width 200ms cubic-bezier(0.7, 0.1, 0.33, 1) 0ms',
                    gap: 12,
                }}
                to='/'
            >
                {/* todo logo */}
                {/* <div
                    style={{
                        flexShrink: 0,
                        display: 'flex',
                        justifyContent: 'center',
                    }}
                >
                    <img
                        style={{
                            width: 26,
                            height: 26,
                            display: 'inline-flex',
                            transition: 'all 250ms cubic-bezier(0.7, 0.1, 0.33, 1) 0ms',
                        }}
                        src={currentThemeType === 'light' ? logo : logoDark}
                        alt='logo'
                    />
                </div> */}
                {ctx.expanded && (
                    <Text
                        size='large'
                        style={{
                            display: 'flex',
                            fontSize: '34px',
                            fontFamily: 'Inter',
                            color: '#fff',
                        }}
                    >
                        LOGO
                    </Text>
                )}
            </Link>
            <div>{currentUser && <HeaderLeftMenu />}</div>
            <div style={{ flexGrow: 1 }} />
            <div
                className={css({
                    'flexShrink': 0,
                    'height': '100%',
                    'font-size': '14px',
                    'color': theme.colors.contentPrimary,
                    'display': 'flex',
                    'align-items': 'center',
                    'gap': '30px',
                })}
            >
                <ThemeToggle />
            </div>
            {currentUser && (
                <div className={styles.userWrapper}>
                    <div className={styles.userAvatarWrapper}>
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
                            <span>{t('logout')}</span>
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
                unstable_ModalBackdropScroll
            >
                <ModalHeader>{t('change password')}</ModalHeader>
                <ModalBody>
                    <PasswordForm onSubmit={handleChangePassword} />
                </ModalBody>
            </Modal>
        </header>
    )
}
function setProjectLoading(isLoading: boolean) {
    throw new Error('Function not implemented.')
}

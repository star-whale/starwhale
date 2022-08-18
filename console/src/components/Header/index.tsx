import React, { useCallback, useState, useContext } from 'react'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { setToken } from '@/api'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { useStyletron } from 'baseui'
import { headerHeight } from '@/consts'
import useTranslation from '@/hooks/useTranslation'
import { createUseStyles } from 'react-jss'
import { IThemedStyleProps } from '@/theme'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import { simulationJump } from '@/utils'
import { BsChevronDown } from 'react-icons/bs'
import { Link, useHistory } from 'react-router-dom'
import PasswordForm from '@user/components/PasswordForm'
import { IChangePasswordSchema } from '@user/schemas/user'
import { changePassword } from '@user/services/user'
import { SidebarContext } from '@/contexts/SidebarContext'
import { toaster } from 'baseui/toast'
import { useCurrentUserRoles } from '@/hooks/useCurrentUserRoles'
import IconFont from '../IconFont'
import Logo from './Logo'

const useHeaderStyles = createUseStyles({
    headerWrapper: {
        padding: '0 20px 0 0',
        height: `${headerHeight}px`,
        width: '100%',
        display: 'flex',
        flexFlow: 'row nowrap',
        alignItems: 'center',
        color: 'var(--color-contentPrimary)',
        backgroundColor: 'var(--color-brandBgNav)',
        position: 'relative',
        zIndex: 10,
    },
})

const useStyles = createUseStyles({
    systemWrapper: {
        'padding-left': '20px',
        'position': 'relative',
        'cursor': 'pointer',
        'display': 'flex',
        'placeItems': 'center',
        'height': '100%',
        '&:hover': {
            '& $systemMenu': {
                display: 'flex',
            },
        },
        '&:before': {
            position: 'absolute',
            content: '""',
            borderLeft: '1px solid #264480',
            left: 0,
            height: '24px',
        },
        '& a': {
            '&:link': {
                'color': '#fff',
                'text-decoration': 'none',
            },
            '&:hover': {
                'color': '#fff',
                'text-decoration': 'none',
            },
            '&:visited': {
                'color': '#fff',
                'text-decoration': 'none',
            },
        },
    },
    systemMenu: (props: IThemedStyleProps) => ({
        'min-width': '180px',
        'position': 'absolute',
        'top': '100%',
        'left': '-50px',
        'display': 'none',
        'margin': 0,
        'padding': '8px 0',
        'line-height': 1.6,
        'flex-direction': 'column',
        'alignItems': 'center',
        'width': '100%',
        'font-size': '13px',
        'borderRadius': '4px',
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
    systemMenuItem: (props: IThemedStyleProps) => ({
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        alignSelf: 'normal',
        gap: '10px',
        height: '32px',
        color: props.theme.colors.contentPrimary,
        backgroundColor: 'var(--color-brandMenuItemBackground)',
    }),
    userWrapper: {
        'position': 'relative',
        'cursor': 'pointer',
        'display': 'flex',
        'align-items': 'center',
        'min-width': '140px',
        'height': '100%',
        'margin-left': '12px',
        'padding': '10px 0 10px 0',
        'justifyContent': 'flex-end',

        '&:hover': {
            '& $userMenu': {
                display: 'flex',
            },
        },
    },
    userNameWrapper: {
        'display': 'flex',
        'color': '#FFF',
        'borderRadius': '20px',
        'fontSize': '16px',
        'padding': '7px 13px 7px 7px',
        'align-items': 'center',
        'height': '36px',
        'gap': '9px',
        'backgroundColor': '#264480',
    },
    userMenu: (props: IThemedStyleProps) => ({
        'position': 'absolute',
        'top': '100%',
        'display': 'none',
        'margin': 0,
        'padding': '8px 0',
        'line-height': 1.6,
        'flex-direction': 'column',
        'alignItems': 'center',
        'width': '100%',
        'font-size': '13px',
        'borderRadius': '4px',
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
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'left',
        alignSelf: 'normal',
        gap: '10px',
        height: '32px',
        paddingLeft: '10px',
        color: props.theme.colors.contentPrimary,
        backgroundColor: 'var(--color-brandMenuItemBackground)',
    }),
    roundWrapper: {
        borderRadius: '50%',
        backgroundColor: 'var(--color-brandWhite)',
        width: '30px',
        height: '30px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
})

export default function Header() {
    const [, theme] = useStyletron()
    const themeType = useCurrentThemeType()
    const styles = useStyles({ theme, themeType })
    const headerStyles = useHeaderStyles({ theme })
    const ctx = useContext(SidebarContext)
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const [currentUserRole] = useCurrentUserRoles()
    const [sysRole, setSysRole] = useState('GUEST')

    const [t] = useTranslation()
    const history = useHistory()

    const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false)
    const handleChangePassword = useCallback(
        async (data: IChangePasswordSchema) => {
            await changePassword(data)
            setIsChangePasswordOpen(false)
            toaster.positive(t('Password Changed'), { autoHideDuration: 2000 })
        },
        [t]
    )

    React.useEffect(() => {
        if (!currentUserRole) {
            return
        }
        // '0' means the system
        const role = currentUserRole.find((i) => i.project.id === '0')
        if (!role) {
            return
        }
        setSysRole(role.role.code)
    }, [currentUserRole, setSysRole])

    return (
        <header className={headerStyles.headerWrapper}>
            <Logo expanded={ctx.expanded} />

            {currentUser && (
                <div className={styles.systemWrapper}>
                    <Link to='/projects'>{t('Project')}</Link>
                </div>
            )}
            <div style={{ flexGrow: 1 }} />

            {currentUser && (
                <div className={styles.userWrapper}>
                    <div className={styles.userNameWrapper}>
                        <div className={styles.roundWrapper}>
                            <IconFont type='user' size={20} kind='white2' />
                        </div>

                        <BsChevronDown size={14} />
                    </div>
                    <div className={styles.userMenu}>
                        {sysRole === 'OWNER' && (
                            <div
                                role='button'
                                tabIndex={0}
                                className={styles.userMenuItem}
                                onClick={() => {
                                    history.push('/admin')
                                }}
                            >
                                <IconFont type='setting' />
                                <span>{t('Admin Settings')}</span>
                            </div>
                        )}
                        <div
                            role='button'
                            tabIndex={0}
                            className={styles.userMenuItem}
                            onClick={() => {
                                setIsChangePasswordOpen(true)
                            }}
                        >
                            <IconFont type='a-passwordresets' />
                            <span>{t('Change Password')}</span>
                        </div>
                        <div
                            role='button'
                            tabIndex={0}
                            className={styles.userMenuItem}
                            onClick={() => {
                                setToken(undefined)
                                simulationJump('/logout')
                            }}
                        >
                            <IconFont type='logout' />
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
                <hr />
                <ModalBody>
                    <PasswordForm currentUser={currentUser} onSubmit={handleChangePassword} />
                </ModalBody>
            </Modal>
        </header>
    )
}

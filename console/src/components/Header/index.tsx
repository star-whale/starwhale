import React, { useCallback, useState, useContext } from 'react'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { headerHeight } from '@/consts'
import useTranslation from '@/hooks/useTranslation'
import { useSearchParam } from 'react-use'
import { createUseStyles } from 'react-jss'
import { useHistory } from 'react-router-dom'
import PasswordForm from '@user/components/PasswordForm'
import { IChangePasswordSchema } from '@user/schemas/user'
import { changePassword } from '@user/services/user'
import { SidebarContext } from '@/contexts/SidebarContext'
import { toaster } from 'baseui/toast'
import { TextLink } from '@/components/Link'
import classNames from 'classnames'
import { useAuth } from '@/api/Auth'
import { useUserRoles } from '@/domain/user/hooks/useUserRoles'
import { Role } from '@/api/const'
import CopyToClipboard from 'react-copy-to-clipboard'
import Button from '@starwhale/ui/Button'
import Input from '@starwhale/ui/Input'
import { useFetchSystemVersion } from '@/domain/setting/hooks/useSettings'
import { IThemedStyleProps } from '@starwhale/ui/theme'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import IconFont from '@starwhale/ui/IconFont'
import Logo from './Logo'
import Avatar from '../Avatar'

const useHeaderStyles = createUseStyles({
    headerWrapper: (props: IThemedStyleProps) => ({
        padding: '0 20px 0 0',
        height: `${headerHeight}px`,
        width: '100%',
        display: 'flex',
        flexFlow: 'row nowrap',
        alignItems: 'center',
        color: props.theme.colors.contentPrimary,
        backgroundColor: props.theme.brandBgNav,
        position: 'relative',
        zIndex: 10,
    }),
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
        backgroundColor: props.theme.brandMenuItemBackground,
    }),
    userWrapper: {
        'position': 'relative',
        'cursor': 'pointer',
        'display': 'flex',
        'align-items': 'center',
        'height': '100%',
        'margin-left': '12px',
        'padding': '10px 0 10px 0',
        'justifyContent': 'flex-end',
        'width': '220px',

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
        'padding': '16px 0px 0px',
        'position': 'absolute',
        'top': '100%',
        'display': 'none',
        'margin': 0,
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
        'backgroundColor': '#FFF',
    }),
    userMenuItems: {
        width: '100%',
        display: 'flex',
        flexShrink: 0,
        flexDirection: 'column',
        overflow: 'hidden',
        overflowY: 'auto',
        background: '#FFFFFF',
        transition: 'all 200ms cubic-bezier(0.7, 0.1, 0.33, 1) 0ms',
        color: 'rgba(2,16,43,0.60)',
        borderRight: '1px solid #E2E7F0',
        padding: '8px 12px 8px',
    },
    userMenuItem: (props: IThemedStyleProps) => ({
        'display': 'flex',
        'alignItems': 'center',
        'justifyContent': 'left',
        'alignSelf': 'normal',
        'gap': '10px',
        'height': '32px',
        'paddingLeft': '10px',
        'color': props.theme.colors.contentPrimary,
        'borderRadius': '4px',
        '&:hover': {
            backgroundColor: props.theme.brandMenuItemBackground,
        },
    }),
    userSignedIn: {
        flex: 1,
        color: 'rgba(2,16,43,0.40)',
        textAlign: 'left',
        width: '100%',
        marginBottom: '13px',
        padding: '0 12px',
    },
    userAvatar: {
        flex: 1,
        width: '100%',
        paddingBottom: '17px',
        gap: '13px',
        display: 'grid',
        gridTemplateColumns: '34px 1fr',
        overflow: 'hidden',
        padding: '0 12px',
    },
    userAvatarInfo: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-start',
        justifyContent: 'center',
    },
    userAvatarName: {
        fontSize: '14px',
        lineHeight: '14px',
        color: '#02102B',
    },
    userAvatarEmail: {
        fontSize: '12px',
        lineHeight: '12px',
        color: 'rgba(2,16,43,0.60)',
    },
    roundWrapper: (props: IThemedStyleProps) => ({
        borderRadius: '50%',
        backgroundColor: props.theme.brandWhite,
        width: '30px',
        height: '30px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    }),
    divider: {
        height: '1px',
        width: '100%',
        backgroundColor: '#EEF1F6',
    },
    version: {
        color: 'rgba(2,16,43,0.60)',
        fontWeight: 600,
        fontSize: '14px',
        paddingLeft: '20px',
        marginTop: '12px',
        textAlign: 'left',
        width: '100%',
    },
})

export default function Header() {
    const [css, theme] = themedUseStyletron()
    const styles = useStyles({ theme })
    const headerStyles = useHeaderStyles({ theme })
    const ctx = useContext(SidebarContext)
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    const cliToken = !!useSearchParam('cli-token')
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { systemRole } = useUserRoles()
    const [t] = useTranslation()
    const history = useHistory()
    const { token, onLogout } = useAuth()
    const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false)
    const [isShowTokenOpen, setIsShowTokenOpen] = useState(cliToken)
    const handleChangePassword = useCallback(
        async (data: IChangePasswordSchema) => {
            await changePassword(data)
            setIsChangePasswordOpen(false)
            toaster.positive(t('Password Changed'), { autoHideDuration: 2000 })
        },
        [t]
    )
    const versionInfo = useFetchSystemVersion()
    const [version, repo] = React.useMemo(() => {
        const [v, ...rest] = versionInfo?.data?.version.split(':') ?? []
        return [v, rest.join('')]
    }, [versionInfo])

    return (
        <header className={headerStyles.headerWrapper}>
            <Logo expanded={ctx.expanded} />

            {currentUser && (
                <div className={styles.systemWrapper}>
                    <TextLink to='/projects' style={{ color: '#FFF' }}>
                        {t('Project List')}
                    </TextLink>
                </div>
            )}
            <div style={{ flexGrow: 1 }} />

            {currentUser && (
                <div className={styles.userWrapper}>
                    <div className={styles.userNameWrapper}>
                        <Avatar name={currentUser.name} size={28} />
                        <IconFont type='arrow_down' />
                    </div>
                    <div className={styles.userMenu}>
                        <p className={styles.userSignedIn}>{t('Signed in as')}</p>
                        <div className={styles.userAvatar}>
                            <Avatar name={currentUser.name} isTooltip={false} />
                            <div className={classNames(styles.userAvatarInfo, 'text-ellipsis')}>
                                <span className={styles.userAvatarName}>{currentUser.name}</span>
                                {currentUser.email && (
                                    <p className={styles.userAvatarEmail}>{currentUser.email ?? ''}</p>
                                )}
                            </div>
                        </div>
                        <div className={styles.divider} />
                        <div className={styles.version} title={repo}>
                            {version}
                        </div>
                        <div className={styles.userMenuItems}>
                            {systemRole === Role.OWNER && (
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
                                    setIsShowTokenOpen(true)
                                }}
                            >
                                <IconFont type='token' />
                                <span>{t('Get Token')}</span>
                            </div>
                        </div>
                        <div className={styles.divider} />
                        <div className={styles.userMenuItems}>
                            <div role='button' tabIndex={0} className={styles.userMenuItem} onClick={onLogout}>
                                <IconFont type='logout' />
                                <span>{t('Logout')}</span>
                            </div>
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
            <Modal animate closeable onClose={() => setIsShowTokenOpen(false)} isOpen={isShowTokenOpen}>
                <ModalHeader>{t('Get Token')}</ModalHeader>
                <ModalBody>
                    <div className={css({ display: 'flex', marginTop: '10px', gap: '10px' })}>
                        <Input value={token ?? ''} />
                        <CopyToClipboard
                            text={token ?? ''}
                            onCopy={() => {
                                toaster.positive(t('Copied'), { autoHideDuration: 1000 })
                            }}
                        >
                            <Button>{t('Copy')}</Button>
                        </CopyToClipboard>
                    </div>
                </ModalBody>
            </Modal>
        </header>
    )
}

import React, { useState } from 'react'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { setToken } from '@/api'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { useStyletron } from 'baseui'
import { headerHeight } from '@/consts'
import useTranslation from '@/hooks/useTranslation'
import { createUseStyles } from 'react-jss'
import { IThemedStyleProps } from '@/theme'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import User from '@/domain/user/components/User'
import { simulationJump } from '@/utils'
import { BsChevronDown } from 'react-icons/bs'
import IconFont from '../IconFont'

const useHeaderStyles = createUseStyles({
    headerWrapper: {
        padding: '0 32px 0 0',
        position: 'absolute',
        zIndex: 100,
        top: 0,
        right: 0,
        height: `${headerHeight}px`,
        width: '30%',
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

        'padding': '14px 0 14px 0',
        '&:hover': {
            '& $userMenu': {
                display: 'flex',
            },
        },
    },
    userNameWrapper: {
        'height': '100%',
        'display': 'flex',
        'color': 'var(--color-brandBgUserFont)',
        'backgroundColor': 'var(--color-brandBgUser)',
        'borderRadius': '20px',
        'fontSize': '16px',
        'padding': '7px 13px 7px 7px',
        'align-items': 'center',
        'lineHeight': '40px',
        'gap': '9px',
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
        justifyContent: 'center',
        alignSelf: 'normal',
        gap: '10px',
        height: '32px',
        color: props.theme.colors.contentPrimary,
        backgroundColor: 'var(--color-brandMenuItemBackground)',
    }),
})

export default function Header() {
    const [, theme] = useStyletron()
    const themeType = useCurrentThemeType()
    const styles = useStyles({ theme, themeType })
    const headerStyles = useHeaderStyles({ theme })
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()

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
            <div style={{ flexGrow: 1 }} />
            {currentUser && (
                <div className={styles.userWrapper}>
                    <div className={styles.userNameWrapper}>
                        <div
                            style={{
                                borderRadius: '50%',
                                backgroundColor: 'var(--color-brandWhite)',
                                width: '30px',
                                height: '30px',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                            }}
                        >
                            <IconFont type='user' size={20} kind='white2' />
                        </div>

                        <User size='large' user={currentUser} />
                        <BsChevronDown size={14} />
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
                <ModalBody>{/* <PasswordForm onSubmit={handleChangePassword} /> */}</ModalBody>
            </Modal>
        </header>
    )
}

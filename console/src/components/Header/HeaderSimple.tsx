import React, { useCallback, useState, useContext, useEffect } from 'react'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
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
import { useFetchSystemFeatures, useFetchSystemVersion } from '@/domain/setting/hooks/useSettings'
import { IThemedStyleProps } from '@starwhale/ui/theme'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import IconFont from '@starwhale/ui/IconFont'
import Logo from './Logo'
import Avatar from '../Avatar'
import LanguageSelector from './LanguageSelector'
import { HeaderExtends } from '../Extensions'
import { useSystemFeatures } from '@/domain/setting/hooks/useSystemFeatures'
import { docsEN, docsZH, headerHeight } from '@/consts'
import i18n from '@/i18n'

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
        'margin-left': '4px',
        'padding': '10px 0 10px 0',
        'justifyContent': 'flex-end',

        '&:hover': {
            '& $userMenu': {
                display: 'flex',
                width: '220px',
            },
        },
    },
    userNameWrapper: {
        'display': 'flex',
        'color': '#FFF',
        'borderRadius': '50%',
        'fontSize': '16px',
        'justifyContent': 'center',
        'align-items': 'center',
        'height': '28px',
        'width': '28px',
        'backgroundColor': '#D0DDF7',
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

export default function HeaderSimple() {
    const [, theme] = themedUseStyletron()
    const headerStyles = useHeaderStyles({ theme })
    const ctx = useContext(SidebarContext)
    const [t] = useTranslation()

    const isZH = i18n.language === 'zh'

    return (
        <header className={headerStyles.headerWrapper}>
            <Logo expanded={ctx.expanded} />
            <div style={{ flexGrow: 1 }} />
            <div
                style={{
                    flexShrink: 0,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    marginRight: '16px',
                }}
            >
                <LanguageSelector />
                <a
                    target='_blank'
                    rel='noreferrer'
                    href={isZH ? docsZH : docsEN}
                    style={{ color: '#FFF', textDecoration: 'none', lineHeight: '20px' }}
                >
                    {t('docs')}
                </a>
            </div>
        </header>
    )
}

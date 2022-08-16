import Card from '@/components/Card'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { ILoginUserSchema } from '@user/schemas/user'
import { loginUser } from '@user/services/user'
import qs from 'qs'
import React, { useCallback, useState } from 'react'
import { Link, useHistory, useLocation } from 'react-router-dom'
import Button from '@/components/Button'
import IconFont from '@/components/IconFont'
import Logo from '@/components/Header/Logo'
import Input from '@/components/Input'
import { BaseNavTabs } from '@/components/BaseNavTabs'
import { INavItem } from '@/components/BaseSidebar'
import { Checkbox } from 'baseui/checkbox'
import { Trans } from 'react-i18next'
import './login.scss'
import { useSearchParam } from 'react-use'
import ThirdPartyLoginButton from '@user/components/ThirdPartyLoginButton'
import { SignupNeedCreateAccount, SignupAccountCreated } from '@/consts'
import { setToken } from '@/api'
import LoginLayout from './LoginLayout'

const { Form, FormItem } = createForm<ILoginUserSchema>()

export default function LoginNew() {
    const [t] = useTranslation()
    const location = useLocation()
    const history = useHistory()
    const [isLoading, setIsLoading] = useState(false)
    const step = useSearchParam('step')
    const token = useSearchParam('token') ?? ''
    if (step === SignupNeedCreateAccount) {
        history.push({
            pathname: '/create-account',
            search: location.search,
        })
    } else if (step === SignupAccountCreated) {
        setToken(token)
        history.push('/')
    }

    const handleRedirect = useCallback(() => {
        const search = qs.parse(location.search, { ignoreQueryPrefix: true })
        let { redirect } = search
        if (redirect && typeof redirect === 'string') {
            redirect = decodeURI(redirect)
        } else {
            redirect = '/'
        }
        history.push(redirect)
    }, [history, location.search])

    const handleFinish = useCallback(
        async (data: ILoginUserSchema) => {
            setIsLoading(true)
            try {
                await loginUser(data)
                handleRedirect()
            } finally {
                setIsLoading(false)
            }
        },
        [handleRedirect]
    )

    const navItems: INavItem[] = React.useMemo(() => {
        const items = [
            {
                title: t('logIn'),
                path: '/loginnew',
                pattern: '/\\/login\\/?',
            },
            {
                title: t('signUp'),
                path: '/signup',
                pattern: '/\\/signup\\/?',
            },
        ]
        return items
    }, [t])

    const isLogin = location.pathname.includes('login')

    return (
        <LoginLayout>
            <div
                style={{
                    display: 'flex',
                    width: '100%',
                    height: '100%',
                    flexDirection: 'row',
                    justifyContent: 'center',
                    position: 'relative',
                    top: '-13px',
                }}
            >
                <div
                    style={{
                        display: 'flex',
                        flexDirection: 'column',
                        justifyContent: 'center',
                    }}
                >
                    <Card
                        style={{
                            paddingTop: '30px',
                            width: 340,
                            borderRadius: 6,
                            boxShadow: '0 2px 8px 0',
                        }}
                        bodyStyle={{
                            padding: 0,
                        }}
                    >
                        <div className='flex-row-center'>
                            <Logo kind='gray' style={{ height: '20px', marginBottom: '18px' }} />
                        </div>
                        <BaseNavTabs
                            fill='fixed'
                            navItems={navItems}
                            tabsOverrides={{
                                TabList: {
                                    style: {
                                        marginBottom: '0',
                                        paddingBottom: 0,
                                    },
                                },
                                TabBorder: {
                                    style: {
                                        height: '1px',
                                    },
                                },
                                TabHighlight: {
                                    style: {
                                        background: '#02102B',
                                        height: '1px',
                                    },
                                },
                            }}
                            tabOverrides={{
                                Tab: {
                                    // @ts-ignore
                                    style: ({ $isActive }) => {
                                        return {
                                            'paddingBottom': '12px',
                                            'paddingTop': '12px',
                                            'color': $isActive ? '#02102B' : 'rgba(2,16,43,0.60)',
                                            ':hover': {
                                                color: '#02102B',
                                            },
                                        }
                                    },
                                },
                            }}
                        />

                        <div
                            style={{
                                padding: '20px 30px 40px',
                            }}
                        >
                            <Form onFinish={handleFinish}>
                                <FormItem>
                                    {/* TODO use the right icons */}
                                    <ThirdPartyLoginButton
                                        isLogin={isLogin}
                                        vendorName={t('Github')}
                                        vendor='github'
                                        icon={<IconFont type='Facebook' />}
                                    />
                                </FormItem>
                                <FormItem>
                                    <ThirdPartyLoginButton
                                        isLogin={isLogin}
                                        vendorName={t('Google')}
                                        vendor='google'
                                        icon={<IconFont type='Instagram' />}
                                    />
                                </FormItem>
                                <FormItem name='userName'>
                                    <Input
                                        placeholder={
                                            isLogin
                                                ? t('emailPlaceholder for login')
                                                : t('emailPlaceholder for sing up')
                                        }
                                        startEnhancer={<IconFont type='email' kind='gray' />}
                                    />
                                </FormItem>
                                <FormItem name='userPwd'>
                                    <Input
                                        startEnhancer={<IconFont type='password' kind='gray' />}
                                        overrides={{
                                            MaskToggleHideIcon: () => <IconFont type='eye_off' kind='gray' />,
                                            MaskToggleShowIcon: () => <IconFont type='eye' kind='gray' />,
                                        }}
                                        type='password'
                                        placeholder={t('passwordPlaceholder')}
                                    />
                                </FormItem>
                                {isLogin || (
                                    <div
                                        style={{
                                            display: 'flex',
                                            gap: '8px',
                                        }}
                                    >
                                        <FormItem name='agreement'>
                                            <Checkbox
                                                overrides={{
                                                    Root: {
                                                        style: {
                                                            alignItems: 'center',
                                                        },
                                                    },
                                                    Checkmark: {
                                                        style: {
                                                            width: '14px',
                                                            height: '14px',
                                                            border: '1px solid #CFD7E6;',
                                                        },
                                                    },
                                                }}
                                            />
                                        </FormItem>
                                        <div className='agreement'>
                                            <p>
                                                <Trans i18nKey='agreePolicy'>
                                                    I agree to <Link to='/'>Terms of Service</Link> and
                                                    <Link to='/'>Privacy Policy</Link>
                                                </Trans>
                                            </p>
                                            <p>
                                                <Trans i18nKey='alreadyHaveAccount'>
                                                    I agree to <Link to='/'>Terms of Service</Link> and
                                                    <Link to='/'>Privacy Policy</Link>
                                                </Trans>
                                                <Link to='/login'>{t('logIn')}</Link>
                                            </p>
                                        </div>
                                    </div>
                                )}
                                <FormItem>
                                    <div style={{ display: 'flex', marginTop: '20px' }}>
                                        <Button
                                            isFull
                                            isLoading={isLoading}
                                            overrides={{ BaseButton: { style: { height: '40px', fontSize: '16px' } } }}
                                        >
                                            {isLogin ? t('logIn') : t('signUp')}
                                        </Button>
                                    </div>
                                </FormItem>
                            </Form>
                        </div>
                    </Card>
                </div>
            </div>
        </LoginLayout>
    )
}

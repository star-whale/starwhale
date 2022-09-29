import Card from '@/components/Card'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { ILoginUserSchema } from '@user/schemas/user'
import { Input } from 'baseui/input'
import React, { useCallback, useState } from 'react'
import { useHistory } from 'react-router-dom'
import Button from '@/components/Button'
import IconFont from '@/components/IconFont'
import { useAuth } from '@/api/Auth'
import LoginLayout from './LoginLayout'

const { Form, FormItem } = createForm<ILoginUserSchema>()

export default function Login() {
    const [t] = useTranslation()
    const history = useHistory()
    const [isLoading, setIsLoading] = useState(false)
    const { onLogin } = useAuth()

    const handleFinish = useCallback(
        async (data: ILoginUserSchema) => {
            setIsLoading(true)
            try {
                const redirect = await onLogin(data)
                history.push(redirect)
            } finally {
                setIsLoading(false)
            }
        },
        [history, onLogin]
    )

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
                    top: '-100px',
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
                            padding: '40px 60px',
                            width: 420,
                        }}
                        bodyStyle={{
                            padding: 0,
                            borderRadius: 12,
                            boxShadow: 'none',
                        }}
                    >
                        <Form onFinish={handleFinish}>
                            <div
                                style={{
                                    fontSize: '28px',
                                    fontWeight: 600,
                                    lineHeight: '28px',
                                    marginBottom: '40px',
                                }}
                            >
                                {t('LOGIN')}
                            </div>
                            <FormItem name='userName' label={t('Username')}>
                                <Input startEnhancer={<IconFont type='user' kind='gray' />} />
                            </FormItem>
                            <FormItem name='userPwd' label={t('Password')}>
                                <Input
                                    startEnhancer={<IconFont type='password' kind='gray' />}
                                    overrides={{
                                        MaskToggleHideIcon: () => <IconFont type='eye_off' kind='gray' />,
                                        MaskToggleShowIcon: () => <IconFont type='eye' kind='gray' />,
                                    }}
                                    type='password'
                                />
                            </FormItem>
                            <FormItem>
                                <div style={{ display: 'flex', marginTop: '40px' }}>
                                    <Button
                                        isFull
                                        isLoading={isLoading}
                                        overrides={{
                                            BaseButton: {
                                                style: {
                                                    height: '40px',
                                                    fontSize: '16px',
                                                },
                                            },
                                        }}
                                    >
                                        {t('login')}
                                    </Button>
                                </div>
                            </FormItem>
                        </Form>
                    </Card>
                </div>
            </div>
        </LoginLayout>
    )
}

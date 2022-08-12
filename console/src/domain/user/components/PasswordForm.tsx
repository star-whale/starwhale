import React, { useState } from 'react'
import { Input } from 'baseui/input'
import { Button } from 'baseui/button'
import { Block } from 'baseui/block'
import useTranslation from '@/hooks/useTranslation'
import { createForm } from '@/components/Form'
import { IChangePasswordSchema, IUserSchema } from '@user/schemas/user'
import { shouldBeEqual, minLength } from '@/components/Form/validators'
import { RadioGroup, Radio } from 'baseui/radio'
import { useStyletron } from 'baseui'
import { checkUserPasswd } from '@user/services/user'
import { passwordMinLength } from '@/consts'

export interface IPasswordFormProps {
    currentUser?: IUserSchema
    admin?: boolean
    onSubmit: (data: IChangePasswordSchema) => Promise<void>
}

interface IInternalPasswordFormProps extends IChangePasswordSchema {
    confirmPassword?: string
}

const useRandomValue = 'yes'
const { Form, FormItem, useForm } = createForm<IInternalPasswordFormProps>()

export default function PasswordForm({ currentUser, admin, onSubmit }: IPasswordFormProps) {
    const [t] = useTranslation()
    const [form] = useForm()
    const [css] = useStyletron()
    const [useRandom, setUseRandom] = useState(false)
    const [passwdValid, setPasswdValid] = useState(!admin)
    const [useRandomRadioVal, setUseRandomRadioVal] = useState('no')

    const handelSubmit = (data: IChangePasswordSchema) => {
        if (!passwdValid) {
            return
        }
        onSubmit(data)
    }

    return (
        <Form form={form} onFinish={handelSubmit}>
            <FormItem label={t('Username')}>
                {/* TODO: use read only input component of baseui v12 */}
                <Block backgroundColor='primary100' padding={['5px', '0', '5px', '5px']}>
                    {currentUser?.name}
                </Block>
            </FormItem>
            <FormItem label={admin ? t('Your Password') : t('Current Password')} name='originPwd' required>
                <div className={css({ display: 'flex', marginTop: '10px' })}>
                    <Input type='password' size='compact' />
                    {admin && (
                        <Button
                            size='compact'
                            onClick={async () => {
                                const pwd = form.getFieldValue('originPwd')?.trim()
                                if (!pwd) {
                                    return
                                }
                                await checkUserPasswd(pwd)
                                setPasswdValid(true)
                            }}
                        >
                            {t('Validate Password')}
                        </Button>
                    )}
                </div>
            </FormItem>
            {/* TODO: refactor password component with NewUserForm */}
            {admin && (
                <RadioGroup
                    align='horizontal'
                    value={useRandomRadioVal}
                    onChange={(e) => {
                        setUseRandomRadioVal(e.target.value)
                        setUseRandom(e.target.value === useRandomValue)
                    }}
                    disabled={!passwdValid}
                >
                    <Radio value='no'>{t('Enter your password')}</Radio>
                    <Radio value={useRandomValue}>{t('Use random password')}</Radio>
                </RadioGroup>
            )}
            {!useRandom && (
                <>
                    <FormItem
                        label={t('New Password')}
                        name='userPwd'
                        required={passwdValid}
                        validators={[minLength(passwordMinLength, t('Password Too Short'))]}
                    >
                        <Input type='password' size='compact' disabled={!passwdValid} />
                    </FormItem>
                    <FormItem
                        label={t('Confirm New Password')}
                        name='confirmPassword'
                        validators={[shouldBeEqual(() => form.getFieldValue('userPwd'), t('Password Not Equal'))]}
                        required={passwdValid}
                    >
                        <Input type='password' size='compact' disabled={!passwdValid} />
                    </FormItem>
                </>
            )}
            <FormItem>
                <div style={{ display: 'flex' }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button size='compact' disabled={!passwdValid}>
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}

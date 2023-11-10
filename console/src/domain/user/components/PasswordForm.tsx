import React, { useState } from 'react'
import { Block } from 'baseui/block'
import useTranslation from '@/hooks/useTranslation'
import { createForm } from '@/components/Form'
import Input from '@starwhale/ui/Input'
import { IChangePasswordSchema } from '@user/schemas/user'
import { minLength, shouldBeEqual } from '@/components/Form/validators'
import { Radio, RadioGroup } from '@starwhale/ui/Radio'
import { useStyletron } from 'baseui'
import { checkUserPasswd } from '@user/services/user'
import { passwordMinLength } from '@/consts'
import Button from '@starwhale/ui/Button'
import { IUserVo } from '@/api'

export interface IPasswordFormProps {
    currentUser?: IUserVo
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
                <div
                    className={css({
                        display: 'grid',
                        marginTop: '10px',
                        gap: '5px',
                        gridTemplateColumns: '1fr 80px',
                    })}
                >
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

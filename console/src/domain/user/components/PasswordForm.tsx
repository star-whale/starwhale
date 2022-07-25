import React from 'react'
import { Input } from 'baseui/input'
import { Button } from 'baseui/button'
import { Block } from 'baseui/block'
import useTranslation from '@/hooks/useTranslation'
import { createForm } from '@/components/Form'
import { IChangePasswordSchema, IUserSchema } from '@user/schemas/user'

export interface IPasswordFormProps {
    currentUser?: IUserSchema
    onSubmit: (data: IChangePasswordSchema) => Promise<void>
}

interface IInternalPasswordFormProps extends IChangePasswordSchema {
    confirmPassword?: string
}

const { Form, FormItem, useForm } = createForm<IInternalPasswordFormProps>()

export default function PasswordForm({ currentUser, onSubmit }: IPasswordFormProps) {
    const [t] = useTranslation()
    const [form] = useForm()

    // check if the two passwords are the same
    const validatePassword = (rule: any, value: any) => {
        if (form.getFieldValue('userPwd') !== value) {
            return Promise.reject(t('password not equal'))
        }
        return Promise.resolve()
    }

    return (
        <Form form={form} onFinish={onSubmit}>
            <FormItem label={t('Username')}>
                {/* TODO: use read only input component of baseui v12 */}
                <Block backgroundColor='primary100' padding={['5px', '0', '5px', '5px']}>
                    {currentUser?.name}
                </Block>
            </FormItem>
            <FormItem label={t('Current Password')} name='originPwd'>
                <Input type='password' size='compact' />
            </FormItem>
            <FormItem label={t('New Password')} name='userPwd'>
                <Input type='password' size='compact' />
            </FormItem>
            <FormItem label={t('Confirm New Password')} name='confirmPassword' validators={[validatePassword]}>
                <Input type='password' size='compact' />
            </FormItem>
            <FormItem>
                <div style={{ display: 'flex' }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button size='compact'>{t('submit')}</Button>
                </div>
            </FormItem>
        </Form>
    )
}

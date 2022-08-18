import React from 'react'
import { createForm } from '@/components/Form'
import Input from '@/components/Input'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import useTranslation from '@/hooks/useTranslation'
import Button from '@/components/Button'
import { shouldBeEqual, minLength } from '@/components/Form/validators'
import { passwordMinLength } from '@/consts'

interface IInternalFormProps {
    password: string
    confirm: string
}

export interface IResetPasswordFormProps {
    title: string
    onSubmit: (password: string) => void
}

const { Form, FormItem, useForm } = createForm<IInternalFormProps>()

export default function ResetPasswordForm({ title, onSubmit }: IResetPasswordFormProps) {
    const [t] = useTranslation()
    const [form] = useForm()

    return (
        <Modal isOpen closeable={false}>
            <ModalHeader>{t('Reset Your Password')}</ModalHeader>
            <ModalBody>
                <div>{t('(reset from) Enter your password For')}</div>
                <div style={{ marginBottom: '20px' }}>{title}</div>
                <Form form={form} onFinish={(props) => onSubmit(props.password)}>
                    <FormItem
                        required
                        name='password'
                        validators={[minLength(passwordMinLength, t('Password Too Short'))]}
                    >
                        <Input placeholder={t('Your Password')} type='password' />
                    </FormItem>
                    <FormItem
                        required
                        name='confirm'
                        validators={[shouldBeEqual(() => form.getFieldValue('password'), t('Password Not Equal'))]}
                    >
                        <Input placeholder={t('Confirm New Password')} type='password' />
                    </FormItem>
                    <FormItem>
                        <div style={{ display: 'flex' }}>
                            <div style={{ flexGrow: 1 }} />
                            <Button size='compact'>{t('submit')}</Button>
                        </div>
                    </FormItem>
                </Form>
            </ModalBody>
        </Modal>
    )
}

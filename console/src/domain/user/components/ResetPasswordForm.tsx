import React from 'react'
import { Button } from 'baseui/button'
import useTranslation from '@/hooks/useTranslation'
import { createForm } from '@/components/Form'
import Input from '@/components/Input'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import IconFont from '@/components/IconFont'

interface IInternalResetPasswordFormProps {
    email: string
}

export interface IResetPasswordFormProps {
    onSubmit: (email: string) => void
}

const { Form, FormItem, useForm } = createForm<IInternalResetPasswordFormProps>()

export default function ResetPasswordForm({ onSubmit }: IResetPasswordFormProps) {
    const [t] = useTranslation()
    const [form] = useForm()

    return (
        <Modal isOpen closeable={false}>
            <ModalHeader>{t('Reset Password Form title')}</ModalHeader>
            <ModalBody>
                <Form form={form} onFinish={(props) => onSubmit(props.email)}>
                    <FormItem name='email'>
                        <Input startEnhancer={<IconFont type='email' kind='gray' />} size='compact' />
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

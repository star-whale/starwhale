import React from 'react'
import { Button } from 'baseui/button'
import useTranslation from '@/hooks/useTranslation'
import { createForm } from '@/components/Form'
import Input from '@starwhale/ui/Input'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import IconFont from '@starwhale/ui/IconFont'

interface ISendEmailFormProps {
    email: string
}

export interface ISendResetPasswordEmailFormProps {
    show: boolean
    onSubmit: (email: string) => void
    cancel: () => void
}

const { Form, FormItem, useForm } = createForm<ISendEmailFormProps>()

export default function SendResetPasswordEmailForm({ show, onSubmit, cancel }: ISendResetPasswordEmailFormProps) {
    const [t] = useTranslation()
    const [form] = useForm()

    return (
        <Modal isOpen={show} onClose={cancel}>
            <ModalHeader>{t('Send Reset Password Email Form title')}</ModalHeader>
            <ModalBody>
                <Form form={form} onFinish={(props) => onSubmit(props.email)}>
                    <FormItem name='email' required>
                        <Input startEnhancer={<IconFont type='email' kind='gray' />} />
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

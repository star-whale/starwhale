import React from 'react'
import { Input } from 'baseui/input'
import { Button } from 'baseui/button'
import useTranslation from '@/hooks/useTranslation'
import { createForm } from '@/components/Form'

interface IInternalAccountFormProps {
    name: string
}

export interface ICreateAccountFormProps {
    method?: string
    onSubmit: (name: string) => void
}

const { Form, FormItem, useForm } = createForm<IInternalAccountFormProps>()

export default function CreateAccountForm({ method, onSubmit }: ICreateAccountFormProps) {
    const [t] = useTranslation()
    const [form] = useForm()

    return (
        <Form form={form} onFinish={(props) => onSubmit(props.name)}>
            {method && <span>{`${t('Log In With')} ${method}`}</span>}
            <FormItem label={t('Account Name')} name='name' required>
                <Input size='compact' />
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

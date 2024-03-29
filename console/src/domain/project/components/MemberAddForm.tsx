import React from 'react'
import { Button } from 'baseui/button'
import useTranslation from '@/hooks/useTranslation'
import { createForm } from '@/components/Form'
import RoleSelector from '@project/components/RoleSelector'
import UserSelector from '@project/components/UserSelector'
import { IUserSchema } from '@user/schemas/user'

export interface IMemberAddSchema {
    userId: string
    roleId: string
}

export interface IMemberAddFromProps {
    users: IUserSchema[]
    onSubmit: (data: IMemberAddSchema) => Promise<void>
}

const { Form, FormItem, useForm } = createForm<IMemberAddSchema>()

export default function MemberAddForm({ users, onSubmit }: IMemberAddFromProps) {
    const [t] = useTranslation()
    const [form] = useForm()

    return (
        <Form form={form} onFinish={onSubmit}>
            <FormItem label={t('Username')} name='userId' required>
                <UserSelector ignoreIds={users.map(({ id }: IUserSchema) => id)} />
            </FormItem>
            <FormItem label={t('Project Role')} name='roleId' required>
                <RoleSelector />
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

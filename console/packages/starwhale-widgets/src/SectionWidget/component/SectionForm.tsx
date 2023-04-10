import React, { useCallback, useEffect, useState } from 'react'
import { createForm } from '@/components/Form'
import { Input } from 'baseui/input'
import useTranslation from '@/hooks/useTranslation'
import Button from '@starwhale/ui/Button'

const { Form, FormItem } = createForm<{ name: string }>()

export default function SectionForm({ formData, onSubmit }: any) {
    const [values, setValues] = useState<any | undefined>(formData)

    useEffect(() => {
        if (!formData) {
            return
        }
        setValues({
            name: formData?.name,
        })
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [formData?.name])

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useCallback((_changes, values_) => {
        setValues(values_)
    }, [])

    const handleFinish = useCallback(
        async (values_) => {
            setLoading(true)
            try {
                await onSubmit(values_)
            } finally {
                setLoading(false)
            }
        },
        [onSubmit]
    )

    const [t] = useTranslation()

    return (
        <Form initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            <FormItem name='name' label={t('sth name')}>
                <Input size='compact' />
            </FormItem>
            <FormItem>
                <div style={{ display: 'flex' }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button isLoading={loading}>{t('submit')}</Button>
                </div>
            </FormItem>
        </Form>
    )
}

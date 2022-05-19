import React, { useCallback, useEffect, useState } from 'react'
import { createForm } from '@/components/Form'
import { Input } from 'baseui/input'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import { isModified } from '@/utils'
import { ICreateBaseImageSchema, IBaseImageSchema } from '@/domain/runtime/schemas/runtime'

const { Form, FormItem } = createForm<ICreateBaseImageSchema>()

export interface IBaseImageFormProps {
    baseImage?: IBaseImageSchema
    onSubmit: (data: ICreateBaseImageSchema) => Promise<void>
}

export default function BaseImageForm({ baseImage, onSubmit }: IBaseImageFormProps) {
    const [values, setValues] = useState<ICreateBaseImageSchema | undefined>({
        imageName: '',
    })

    useEffect(() => {
        if (!baseImage) {
            return
        }
        setValues({
            imageName: baseImage.name,
        })
    }, [baseImage])

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
            <FormItem name='imageName' label={t('sth name', [t('BaseImage')])}>
                <Input />
            </FormItem>
            <FormItem>
                <div style={{ display: 'flex' }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button isLoading={loading} disabled={!isModified(baseImage, values)}>
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}

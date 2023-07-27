import React, { useEffect } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { FormInstance, FormItemProps } from '@/components/Form/form'
import { ICreateJobFormSchema } from '../schemas/job'
import ResourcePoolSelector from '@/domain/setting/components/ResourcePoolSelector'
import { useQueryArgs } from '@/hooks/useQueryArgs'

function FormFieldResource({
    form,
    FormItem,
    setResource,
}: {
    form: FormInstance<ICreateJobFormSchema, keyof ICreateJobFormSchema>
    FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any
    setResource: (resource: any) => void
}) {
    const [t] = useTranslation()
    const { query } = useQueryArgs()
    const { resourcePool } = query

    useEffect(() => {
        form.setFieldsValue({
            resourcePool,
        })
    }, [form, resourcePool])

    return (
        <div
            style={{
                display: 'grid',
                gap: 40,
                gridTemplateColumns: '280px 300px 280px',
            }}
        >
            <FormItem label={t('Resource Pool')} name='resourcePool' required>
                <ResourcePoolSelector
                    autoSelected
                    onChangeItem={(item) => {
                        setResource(item as any)
                    }}
                />
            </FormItem>
        </div>
    )
}

export default FormFieldResource

import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { FormInstance, FormItemProps } from '@/components/Form/form'
import { ICreateJobFormSchema } from '../schemas/job'
import ResourcePoolSelector from '@/domain/setting/components/ResourcePoolSelector'

function FormFieldResource({
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    form,
    FormItem,
    setResource,
}: {
    form: FormInstance<ICreateJobFormSchema, keyof ICreateJobFormSchema>
    FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any
    setResource: (resource: any) => void
}) {
    const [t] = useTranslation()

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

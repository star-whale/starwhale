import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { FormInstance, FormItemProps } from '@/components/Form/form'
import { useParams } from 'react-router-dom'
import { ICreateJobFormSchema } from '../schemas/job'
import DatasetTreeSelector from '@/domain/dataset/components/DatasetTreeSelector'

function FormFieldDataset({
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    form,
    FormItem,
    label,
    name = 'datasetVersionUrls',
    required = true,
}: {
    form: FormInstance<ICreateJobFormSchema, keyof ICreateJobFormSchema>
    FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any
    label?: string
    name?: keyof ICreateJobFormSchema
    required?: boolean
}) {
    const [t] = useTranslation()
    const { projectId } = useParams<{ projectId: string }>()

    return (
        <div className='bfc' style={{ width: '660px', marginBottom: '36px' }}>
            <FormItem label={label || t('Dataset Version')} name={name} required={required}>
                <DatasetTreeSelector projectId={projectId} multiple />
            </FormItem>
        </div>
    )
}

export default FormFieldDataset

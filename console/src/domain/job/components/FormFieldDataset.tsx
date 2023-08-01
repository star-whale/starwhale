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
}: {
    form: FormInstance<ICreateJobFormSchema, keyof ICreateJobFormSchema>
    FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any
}) {
    const [t] = useTranslation()
    const { projectId } = useParams<{ projectId: string }>()

    return (
        <div className='bfc' style={{ width: '660px', marginBottom: '36px' }}>
            <FormItem label={t('Dataset Version')} name='datasetVersionUrls' required>
                <DatasetTreeSelector projectId={projectId} multiple />
            </FormItem>
        </div>
    )
}

export default FormFieldDataset

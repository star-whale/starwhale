import React, { useEffect } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { FormInstance, FormItemProps } from '@/components/Form/form'
import { useParams } from 'react-router-dom'
import { ICreateJobFormSchema } from '../schemas/job'
import DatasetTreeSelector from '@/domain/dataset/components/DatasetTreeSelector'

function FormFieldDataset({
    form,
    FormItem,
    datasetVersionUrls,
}: {
    form: FormInstance<ICreateJobFormSchema, keyof ICreateJobFormSchema>
    FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any
    datasetVersionUrls: string
}) {
    const [t] = useTranslation()
    const { projectId } = useParams<{ projectId: string }>()

    useEffect(() => {
        form.setFieldsValue({
            datasetVersionUrls:
                typeof datasetVersionUrls === 'string' ? datasetVersionUrls.split(',') : datasetVersionUrls,
        })
    }, [form, datasetVersionUrls])

    return (
        <div className='bfc' style={{ width: '660px', marginBottom: '36px' }}>
            <FormItem label={t('Dataset Version')} name='datasetVersionUrls' required>
                <DatasetTreeSelector projectId={projectId} multiple getId={(obj) => obj.versionName} />
            </FormItem>
        </div>
    )
}

export default FormFieldDataset

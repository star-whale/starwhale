import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { FormItemProps } from '@/components/Form/form'
import { useParams } from 'react-router-dom'
import { ICreateJobFormSchema } from '../schemas/job'
import JobTemplateSelector from './JobTemplateSelector'

function FormFieldTemplate({ FormItem }: { FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any }) {
    const [t] = useTranslation()
    const { projectId } = useParams<{ projectId: string }>()

    return (
        <div className='bfc' style={{ width: '280px', marginBottom: '36px' }}>
            <FormItem label={t('job.form.template')} name='templateId'>
                <JobTemplateSelector projectId={projectId} />
            </FormItem>
        </div>
    )
}

export default FormFieldTemplate

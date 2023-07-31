import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { FormInstance, FormItemProps } from '@/components/Form/form'
import { ICreateJobFormSchema } from '../schemas/job'
import { Toggle } from '@starwhale/ui/Select'
import { NumberInput } from '@starwhale/ui/Input'

function FormFieldAutoRelease({
    form,
    FormItem,
}: {
    form: FormInstance<ICreateJobFormSchema, keyof ICreateJobFormSchema>
    FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any
}) {
    const [t] = useTranslation()

    return (
        <>
            {form.getFieldValue('isTimeToLiveInSec') && (
                <p style={{ marginBottom: '10px' }}>{t('job.autorelease.notice')}</p>
            )}
            <div style={{ width: '660px', marginBottom: '36px', display: 'flex', gap: '40px' }}>
                <FormItem label={t('job.autorelease.toggle')} name='isTimeToLiveInSec'>
                    <Toggle />
                </FormItem>
                {form.getFieldValue('isTimeToLiveInSec') && (
                    <FormItem label={t('job.autorelease.time')} name='timeToLiveInSec' initialValue={60 * 60}>
                        <NumberInput endEnhancer={() => t('resource.price.unit.second')} />
                    </FormItem>
                )}
            </div>
        </>
    )
}

export default FormFieldAutoRelease

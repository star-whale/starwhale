import React, { useEffect } from 'react'
import { FormSelect } from '@starwhale/ui/Select'
import RuntimeTreeSelector from '@runtime/components/RuntimeTreeSelector'
import useTranslation from '@/hooks/useTranslation'
import { FormInstance, FormItemProps } from '@/components/Form/form'
import { useParams } from 'react-router-dom'
import { ICreateJobFormSchema, RuntimeType } from '../schemas/job'

function FormFieldRuntime({
    form,
    FormItem,
    builtInRuntime,
    runtimeType,
}: {
    form: FormInstance<ICreateJobFormSchema, keyof ICreateJobFormSchema>
    FormItem: (props_: FormItemProps<ICreateJobFormSchema>) => any
    builtInRuntime?: string
    runtimeType?: RuntimeType | string
}) {
    const [t] = useTranslation()
    const { projectId } = useParams<{ projectId: string }>()

    const type = form.getFieldValue('runtimeType')

    useEffect(() => {
        form.setFieldsValue({ runtimeVersionUrl: builtInRuntime })
    }, [form, builtInRuntime])

    useEffect(() => {
        form.setFieldsValue({ runtimeType })
    }, [form, runtimeType])

    return (
        <div className='bfc' style={{ width: '660px', marginBottom: '36px' }}>
            {!!builtInRuntime && (
                <FormItem label={t('Runtime Type')} name='runtimeType'>
                    {/* @ts-ignore */}
                    <FormSelect
                        clearable={false}
                        options={[
                            {
                                label: t('runtime.image.builtin'),
                                id: RuntimeType.BUILTIN,
                            },
                            {
                                label: t('runtime.image.other'),
                                id: RuntimeType.OTHER,
                            },
                        ]}
                    />
                </FormItem>
            )}
            {type === RuntimeType.BUILTIN && (
                <>
                    <label
                        htmlFor='l-built-in'
                        style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '10px 0px' }}
                    >
                        * {t('Runtime Version')}
                    </label>
                    <p
                        id='l-built-in'
                        style={{
                            padding: '5px 20px',
                            borderRadius: '4px',
                            border: '1px solid #E2E7F0',
                        }}
                    >
                        {builtInRuntime}
                    </p>
                </>
            )}
            {(type === RuntimeType.OTHER || !builtInRuntime) && (
                <FormItem label={t('Runtime Version')} name='runtimeVersionUrl' required>
                    <RuntimeTreeSelector projectId={projectId} getId={(obj) => obj.versionName} />
                </FormItem>
            )}
        </div>
    )
}

export default FormFieldRuntime

import React, { useCallback, useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { createForm } from '@/components/Form'
import RuntimeSelector from '@runtime/components/RuntimeSelector'
import RuntimeVersionSelector from '@runtime/components/RuntimeVersionSelector'
import { useParams } from 'react-router-dom'
import ModelSelector from '@model/components/ModelSelector'
import ModelVersionSelector from '@model/components/ModelVersionSelector'
import ResourcePoolSelector from '@/domain/setting/components/ResourcePoolSelector'
import { ICreateOnlineEvalSchema } from '@model/schemas/model'
import { createUseStyles } from 'react-jss'
import { FormInstance } from 'rc-field-form'

interface ICreateOnlineEvalProps {
    modelId: string
    modelVersionUrl: string
    runtimeId: string
    runtimeVersionUrl: string
    resourcePool: string
    ttl: number
}

export interface ICreateOnlineEvalFormProps {
    onSubmit: (data: ICreateOnlineEvalSchema) => void
}

const useStyles = createUseStyles({
    row3: {
        display: 'grid',
        gap: 40,
        gridTemplateColumns: '280px 300px 280px',
    },
})

const { Form, FormItem, useForm } = createForm<ICreateOnlineEvalProps>()

function CreateOnlineEvalForm({ onSubmit }: ICreateOnlineEvalFormProps, formRef: React.RefObject<any>) {
    const [form] = useForm()
    const {
        projectId,
        modelId: $modelId,
        modelVersionId: $modelVersionId,
    } = useParams<{
        projectId: string
        modelId: string
        modelVersionId: string
    }>()
    form.setFieldsValue({ modelId: $modelId, modelVersionUrl: $modelVersionId })

    const styles = useStyles()
    const [t] = useTranslation()

    const [modelId, setModelId] = useState($modelId)
    const [runtimeId, setRuntimeId] = useState('')

    const handleValuesChange = useCallback((_changes, values_) => {
        if (values_.modelId) setModelId(values_.modelId)
        if (values_.runtimeId) setRuntimeId(values_.runtimeId)
    }, [])

    const handleSubmit = useCallback(
        (props) => {
            onSubmit({
                modelVersionUrl: props.modelVersionUrl,
                runtimeVersionUrl: props.runtimeVersionUrl,
                resourcePool: props.resourcePool,
                ttlInSeconds: +props.ttl,
            })
        },
        [onSubmit]
    )

    React.useImperativeHandle(formRef, () => form, [form])

    return (
        <Form form={form} onFinish={handleSubmit} onValuesChange={handleValuesChange}>
            <div className={styles.row3}>
                <FormItem label={t('sth name', [t('Model')])} name='modelId' required>
                    <ModelSelector projectId={projectId} />
                </FormItem>
                {modelId && (
                    <FormItem label={t('Version')} required name='modelVersionUrl'>
                        <ModelVersionSelector projectId={projectId} modelId={modelId} autoSelected />
                    </FormItem>
                )}
            </div>
            <div className={styles.row3}>
                <FormItem label={t('Runtime')} name='runtimeId' required>
                    <RuntimeSelector projectId={projectId} />
                </FormItem>
                {runtimeId && (
                    <FormItem label={t('Version')} required name='runtimeVersionUrl'>
                        <RuntimeVersionSelector projectId={projectId} runtimeId={runtimeId} autoSelected />
                    </FormItem>
                )}
            </div>
            <div className={styles.row3}>
                <FormItem label={t('Resource Pool')} name='resourcePool' required>
                    <ResourcePoolSelector autoSelected />
                </FormItem>
            </div>
        </Form>
    )
}

export default React.forwardRef(CreateOnlineEvalForm)

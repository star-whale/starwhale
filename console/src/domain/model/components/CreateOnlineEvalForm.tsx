import React, { useCallback, useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { createForm } from '@/components/Form'
import RuntimeSelector from '@runtime/components/RuntimeSelector'
import RuntimeVersionSelector from '@runtime/components/RuntimeVersionSelector'
import { useHistory, useParams } from 'react-router-dom'
import ModelSelector from '@model/components/ModelSelector'
import ModelVersionSelector from '@model/components/ModelVersionSelector'
import ResourcePoolSelector from '@/domain/setting/components/ResourcePoolSelector'
import { ICreateOnlineEvalSchema } from '@model/schemas/model'
import { createUseStyles } from 'react-jss'
import FlatResourceSelector, { Dict } from '@/domain/setting/components/FlatResourceSelector'
import { ISystemResourcePool } from '@/domain/setting/schemas/system'
import Toggle from '@starwhale/ui/Select/Toggle'

interface ICreateOnlineEvalProps {
    modelId: string
    modelVersionUrl: string
    runtimeId: string
    runtimeVersionUrl: string
    resourcePool: string
    advance: boolean
    resourceAmount: Dict<string>
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

function CreateOnlineEvalForm({ onSubmit }: ICreateOnlineEvalFormProps, formRef: React.ForwardedRef<any>) {
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
    const history = useHistory()

    const [modelId, setModelId] = useState($modelId)
    const [runtimeId, setRuntimeId] = useState('')
    const [resourcePool, setResourcePool] = React.useState<ISystemResourcePool | undefined>()
    const [showAdvanced, setShowAdvanced] = useState(false)

    const handleValuesChange = useCallback(
        (changes, values) => {
            if (values.modelId) setModelId(values.modelId)
            if (values.runtimeId) setRuntimeId(values.runtimeId)
            setShowAdvanced(values.advance)
            if (!values.advance || 'resourcePool' in changes) {
                // empty the resource amounts when no advance or resource pool changes
                form.resetFields(['resourceAmount'])
            }

            // update uri if model version changed
            if ('modelVersionUrl' in changes) {
                history.push(`/projects/${projectId}/online_eval/${modelId}/${changes.modelVersionUrl}`)
            }
        },
        [form, history, modelId, projectId]
    )

    const handleSubmit = useCallback(
        (props) => {
            onSubmit({
                modelVersionUrl: props.modelVersionUrl,
                runtimeVersionUrl: props.runtimeVersionUrl,
                resourcePool: props.resourcePool,
                // TODO: add spec
                spec: '',
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
                <FormItem label={t('online eval.advance')} name='advance'>
                    <Toggle />
                </FormItem>
            </div>
            {showAdvanced && (
                <>
                    <div className={styles.row3}>
                        <FormItem label={t('Resource Pool')} name='resourcePool' required>
                            <ResourcePoolSelector autoSelected onChangeItem={setResourcePool} />
                        </FormItem>
                    </div>
                    <div className={styles.row3}>
                        <FormItem
                            label={t('Resource Amount')}
                            name='resourceAmount'
                            validators={[
                                (_, resourceAmount) => {
                                    // eslint-disable-next-line no-restricted-syntax,guard-for-in
                                    for (const k in resourceAmount) {
                                        const v: string = resourceAmount[k]
                                        if (Number.isNaN(Number(v))) {
                                            return Promise.reject(Error(`${k} is not a valid number`))
                                        }
                                    }
                                    return Promise.resolve()
                                },
                            ]}
                        >
                            <FlatResourceSelector
                                resourceTypes={resourcePool?.resources || []}
                                className={styles.row3}
                            />
                        </FormItem>
                    </div>
                </>
            )}
        </Form>
    )
}

export default React.forwardRef(CreateOnlineEvalForm)

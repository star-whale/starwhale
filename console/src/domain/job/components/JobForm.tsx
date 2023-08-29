import React, { useCallback, useEffect, useReducer, useState } from 'react'
import { useHistory } from 'react-router-dom'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import Divider from '@/components/Divider'
import { IModelVersionSchema, StepSpec } from '@/domain/model/schemas/modelVersion'
import yaml from 'js-yaml'
import { toaster } from 'baseui/toast'
import Button from '@starwhale/ui/Button'
import { ICreateJobFormSchema, ICreateJobSchema, IJobSchema, RuntimeType } from '../schemas/job'
import { IModelTreeSchema } from '@/domain/model/schemas/model'
import { useQueryArgs } from '@starwhale/core/utils'
import FormFieldRuntime from './FormFieldRuntime'
import { useEventEmitter } from 'ahooks'
import FormFieldModel from './FormFieldModel'
import FormFieldDataset from './FormFieldDataset'
import FormFieldDevMode from './FormFieldDevMode'
import { FormFieldAutoReleaseExtend, FormFieldPriExtend, FormFieldResourceExtend } from '@/components/Extensions'
import { useMachine } from '@xstate/react'
import { jobCreateMachine } from '../createJobMachine'

const { Form, FormItem, useForm } = createForm<ICreateJobFormSchema>()

export interface IJobFormProps {
    job?: IJobSchema
    onSubmit: (data: ICreateJobSchema) => Promise<void>
    autoFill?: boolean
}

export default function JobForm({ job, onSubmit, autoFill = true }: IJobFormProps) {
    const eventEmitter = useEventEmitter<{ changes: Partial<ICreateJobFormSchema>; values: ICreateJobFormSchema }>()
    const [values, setValues] = useState<ICreateJobFormSchema | undefined>(undefined)
    const [modelTree, setModelTree] = useState<IModelTreeSchema[]>([])
    const [resource, setResource] = React.useState<any>()
    const [, forceUpdate] = useReducer((x) => x + 1, 0)
    const [loading, setLoading] = useState(false)
    const { query } = useQueryArgs()
    const [t] = useTranslation()
    const history = useHistory()
    const [form] = useForm()

    const [, send, service] = useMachine(jobCreateMachine)

    const modelVersionHandler = form.getFieldValue('modelVersionHandler')
    const stepSpecOverWrites = form.getFieldValue('stepSpecOverWrites') as string
    const _modelVersionId = form.getFieldValue('modelVersionUrl')

    const modelVersion: IModelVersionSchema | undefined = React.useMemo(() => {
        if (!modelTree || !_modelVersionId) return undefined
        let version: IModelVersionSchema | undefined
        modelTree?.forEach((v) =>
            v.versions.forEach((versionTmp) => {
                if (versionTmp.id === _modelVersionId) {
                    version = versionTmp
                }
            })
        )
        return version
    }, [modelTree, _modelVersionId])

    const fullStepSource: StepSpec[] | undefined = React.useMemo(() => {
        if (!modelVersion) return undefined
        return modelVersion?.stepSpecs ?? []
    }, [modelVersion])

    const stepSource: StepSpec[] | undefined = React.useMemo(() => {
        if (!fullStepSource) return undefined
        if (stepSpecOverWrites) {
            try {
                return (yaml.load(stepSpecOverWrites) ?? []) as StepSpec[]
            } catch (e) {
                return []
            }
        }
        return fullStepSource.filter((v: StepSpec) => v?.job_name === modelVersionHandler)
    }, [fullStepSource, modelVersionHandler, stepSpecOverWrites])

    const checkStepSource = useCallback(
        (value) => {
            try {
                yaml.load(value)
            } catch (e) {
                toaster.negative(t('wrong yaml syntax'), { autoHideDuration: 1000, key: 'yaml' })
                return false
            }
            return true
        },
        [t]
    )

    const handleFinish = useCallback(
        async (values_: ICreateJobFormSchema) => {
            send('USEREDITING')
            setLoading(true)
            const tmp = {
                datasetVersionUrls: values_.datasetVersionUrls?.join(','),
                resourcePool: resource?.resourceId ?? resource?.name,
                stepSpecOverWrites: values_.stepSpecOverWrites,
                runtimeVersionUrl: values_.runtimeType === RuntimeType.BUILTIN ? '' : values_.runtimeVersionUrl,
                modelVersionUrl: values_.modelVersionUrl,
                devMode: values_.devMode,
                devPassword: values_.devPassword,
                timeToLiveInSec: values_.timeToLiveInSec,
            }
            if (values_.rawType && !checkStepSource(values_.stepSpecOverWrites)) {
                setLoading(false)
                return
            }
            try {
                await onSubmit(tmp)
                history.goBack()
            } finally {
                setLoading(false)
            }
        },
        [send, onSubmit, history, checkStepSource, resource]
    )

    const handleValuesChange = useCallback(
        (_changes: Partial<ICreateJobFormSchema>, values_: ICreateJobFormSchema) => {
            eventEmitter.emit({
                changes: _changes,
                values: values_,
            })
            if ('modelVersionUrl' in _changes) {
                send('MODELCHANGED')
            }
            setValues({
                ...values_,
            })
        },
        [send, eventEmitter]
    )

    const sharedFormProps = { form, FormItem, eventEmitter, forceUpdate, autoFill }
    const getModelProps = () => ({
        setModelTree: (v: any) => {
            send('MODELTREEFETCHED', {
                data: v,
            })
            setModelTree(v)
        },
        fullStepSource,
        stepSource,
    })
    const getResourcePoolProps = () => ({ resource, setResource })
    const getRuntimeProps = () => ({ builtInRuntime: modelVersion?.builtInRuntime })

    const isModifiedDataset = React.useMemo(() => {
        return stepSource?.some((v) => v.require_dataset === null || v.require_dataset)
    }, [stepSource])

    useEffect(() => {
        if (!job) return
        send('APIRERUN', {
            data: job,
        })
    }, [job, send])

    useEffect(() => {
        if (!query.modelId) return
        send('QUERYMODELID', {
            data: { modelId: query.modelId, modelVersionHandler: query.modelVersionHandler },
        })
    }, [send, modelTree, query.modelId, query.modelVersionHandler])

    useEffect(() => {
        const subscription = service.subscribe((curr) => {
            const ctx = curr.context
            // console.log(curr.value, ctx)
            // simple state logging
            switch (curr.value) {
                case 'rerunFilled': {
                    const tmp = ctx.job
                    if (!tmp) break
                    // eslint-disable-next-line
                    let modelVersion: IModelVersionSchema | undefined
                    ctx.modelTree?.forEach((v) =>
                        v.versions.forEach((versionTmp) => {
                            if (versionTmp.id === tmp.model?.version?.id) {
                                modelVersion = versionTmp
                            }
                        })
                    )

                    form.setFieldsValue({
                        runtimeType: tmp.isBuiltinRuntime ? RuntimeType.BUILTIN : RuntimeType.OTHER,
                        runtimeVersionUrl: tmp.runtime?.version?.id,
                        resourcePool: tmp.resourcePool,
                        datasetVersionUrls: tmp.datasetList?.map((v) => v.version?.id) as string[],
                        modelVersionUrl: modelVersion ? tmp.model?.version?.id : undefined,
                        modelVersionHandler: tmp.jobName,
                        stepSpecOverWrites: tmp.stepSpec,
                    })
                    forceUpdate()
                    send('USEREDITING')
                    break
                }
                case 'editFilled':
                case 'autoFilled': {
                    // eslint-disable-next-line
                    let modelVersion: IModelVersionSchema | undefined
                    ctx.modelTree?.forEach((v) =>
                        v.versions.forEach((versionTmp) => {
                            if (versionTmp.id === form.getFieldValue('modelVersionUrl')) {
                                modelVersion = versionTmp
                            }
                        })
                    )
                    if (!modelVersion) return

                    const toUpdate: Partial<ICreateJobFormSchema> = {
                        runtimeType: modelVersion?.builtInRuntime ? RuntimeType.BUILTIN : RuntimeType.OTHER,
                    }
                    if (modelVersion?.builtInRuntime) {
                        toUpdate.runtimeVersionUrl = modelVersion?.builtInRuntime
                    } else {
                        toUpdate.runtimeVersionUrl = ''
                    }
                    const handler = modelVersion.stepSpecs?.find((v) => v)?.job_name

                    form.setFieldsValue({
                        ...toUpdate,
                        modelVersionHandler: handler,
                        stepSpecOverWrites: yaml.dump(
                            modelVersion.stepSpecs.filter((v: StepSpec) => v?.job_name === handler)
                        ),
                    })

                    forceUpdate()
                    if (curr.value === 'autoFilled') {
                        send('USEREDITING')
                    }
                    break
                }
                case 'autoFilledByModelId': {
                    if (!ctx.modelTree) return
                    let tmp: IModelVersionSchema | undefined
                    if (ctx.modelTree && ctx.modelId) {
                        ctx.modelTree?.forEach((v) => {
                            if (v.modelId === ctx.modelId) {
                                tmp = v.versions?.[0]
                            }
                        })
                    }
                    const handler = ctx.modelVersionHandler || tmp?.stepSpecs?.find((v) => v)?.job_name
                    if (tmp) {
                        form.setFieldsValue({
                            modelVersionUrl: tmp.id,
                            modelVersionHandler: handler,
                            stepSpecOverWrites: yaml.dump(
                                tmp.stepSpecs.filter((v: StepSpec) => v?.job_name === handler)
                            ),
                        })
                    }

                    forceUpdate()
                    send('USEREDITING')
                    break
                }
                default:
                    break
            }
        })

        return () => {
            send('RESET')
            subscription.unsubscribe()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [service])

    return (
        <Form form={form} initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            {/* env config */}
            <Divider orientation='top'>{t('Environment')}</Divider>
            <FormFieldResourceExtend {...sharedFormProps} {...getResourcePoolProps()} />
            {/* model config */}
            <Divider orientation='top'>{t('Model Information')}</Divider>
            <FormFieldModel {...sharedFormProps} {...getModelProps()} />
            {/* dataset config */}
            {isModifiedDataset && <Divider orientation='top'>{t('Datasets')}</Divider>}
            {isModifiedDataset && <FormFieldDataset {...sharedFormProps} />}
            {/* runtime config */}
            <Divider orientation='top'>{t('Runtime')}</Divider>
            <FormFieldRuntime {...sharedFormProps} {...getRuntimeProps()} />
            {/* advanced config */}
            <Divider orientation='top'>{t('job.advanced')}</Divider>
            {/* debug config */}
            <FormFieldDevMode {...sharedFormProps} />
            {/* auto release time config */}
            <FormFieldAutoReleaseExtend {...sharedFormProps} />
            <FormFieldPriExtend {...sharedFormProps} {...getResourcePoolProps()} />
            <FormItem>
                <div style={{ display: 'flex', gap: 20, marginTop: 60 }}>
                    <Button
                        kind='secondary'
                        type='button'
                        onClick={() => {
                            history.goBack()
                        }}
                    >
                        {t('Cancel')}
                    </Button>
                    <Button isLoading={loading}>{t('submit')}</Button>
                </div>
            </FormItem>
        </Form>
    )
}

import React, { useCallback, useEffect, useReducer, useState } from 'react'
import { useHistory } from 'react-router-dom'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { isModified } from '@/utils'
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

const { Form, FormItem, useForm } = createForm<ICreateJobFormSchema>()

export interface IJobFormProps {
    job?: IJobSchema
    onSubmit: (data: ICreateJobSchema) => Promise<void>
}

export default function JobForm({ job, onSubmit }: IJobFormProps) {
    const EventEmitter = useEventEmitter<{ changes: Partial<ICreateJobFormSchema>; values: ICreateJobFormSchema }>()
    const [values, setValues] = useState<ICreateJobFormSchema | undefined>(undefined)
    const [modelTree, setModelTree] = useState<IModelTreeSchema[]>([])
    const [resource, setResource] = React.useState<any>()
    const [, forceUpdate] = useReducer((x) => x + 1, 0)
    const [loading, setLoading] = useState(false)
    const { query } = useQueryArgs()
    const [t] = useTranslation()
    const history = useHistory()
    const [form] = useForm()

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
        [onSubmit, history, checkStepSource, resource]
    )

    const handleValuesChange = useCallback(
        (_changes: Partial<ICreateJobFormSchema>, values_: ICreateJobFormSchema) => {
            EventEmitter.emit({
                changes: _changes,
                values: values_,
            })
            setValues({
                ...values_,
            })
        },
        [EventEmitter]
    )

    const sharedFormProps = { form, FormItem, EventEmitter, forceUpdate }
    const getModelProps = () => ({ setModelTree, fullStepSource, stepSource })
    const getResourcePoolProps = () => ({ resource, setResource })
    const getRuntimeProps = () => ({ builtInRuntime: modelVersion?.builtInRuntime })

    const isModifiedDataset = React.useMemo(() => {
        return stepSource?.some((v) => v.require_dataset === null || v.require_dataset)
    }, [stepSource])

    useEffect(() => {
        // init by new model
        if (modelVersion) {
            form.setFieldsValue({
                runtimeVersionUrl: modelVersion?.builtInRuntime,
                runtimeType: modelVersion?.builtInRuntime ? RuntimeType.BUILTIN : RuntimeType.OTHER,
            })
        }
        if (modelVersion) {
            form.setFieldsValue({
                modelVersionHandler: modelVersion.stepSpecs?.find((v) => v)?.job_name ?? '',
            })
        }
        // init by online eval args, auto select the first version
        if (modelTree && query.modelId) {
            modelTree?.forEach((v) => {
                if (v.modelId === query.modelId) {
                    form.setFieldsValue({
                        modelVersionUrl: v.versions?.[0]?.id,
                    })
                }
            })
        }
        if (query.modelVersionHandler) {
            form.setFieldsValue({
                modelVersionHandler: query.modelVersionHandler,
            })
        }
        forceUpdate()
    }, [form, query, modelTree, modelVersion])

    useEffect(() => {
        // init by rerun job details
        if (!job) return
        form.setFieldsValue({
            runtimeType: job.isBuiltinRuntime ? RuntimeType.BUILTIN : RuntimeType.OTHER,
            runtimeVersionUrl: job.runtime?.version?.id,
            resourcePool: job.resourcePool,
            datasetVersionUrls: job.datasetList?.map((v) => v.version?.id) as string[],
            modelVersionUrl: job.model?.version?.id,
            modelVersionHandler: job.jobName,
        })
        forceUpdate()
    }, [form, job])

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
                    <Button isLoading={loading} disabled={!isModified(job, values)}>
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}

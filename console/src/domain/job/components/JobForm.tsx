import React, { useCallback, useReducer, useState } from 'react'
import { useHistory } from 'react-router-dom'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { isModified } from '@/utils'
import Divider from '@/components/Divider'
import ResourcePoolSelector from '@/domain/setting/components/ResourcePoolSelector'
import { IModelVersionSchema, StepSpec } from '@/domain/model/schemas/modelVersion'
import yaml from 'js-yaml'
import { createUseStyles } from 'react-jss'
import { toaster } from 'baseui/toast'
import Button from '@starwhale/ui/Button'
import { ICreateJobFormSchema, ICreateJobSchema, IJobSchema, RuntimeType } from '../schemas/job'
import { Toggle } from '@starwhale/ui/Select'
import { IModelTreeSchema } from '@/domain/model/schemas/model'
import Input, { NumberInput } from '@starwhale/ui/Input'
import generatePassword from '@/utils/passwordGenerator'
import CopyToClipboard from 'react-copy-to-clipboard'
import { IconFont } from '@starwhale/ui'
import { WithCurrentAuth } from '@/api/WithAuth'
import { useQueryArgs } from '@starwhale/core/utils'
import FormFieldRuntime from './FormFieldRuntime'
import { useEventEmitter } from 'ahooks'
import FormFieldModel from './FormFieldModel'
import FormFieldDataset from './FormFieldDataset'
import FormFieldDevMode from './FormFieldDevMode'
import FormFieldAutoRelease from './FormFieldAutoRelease'
import { FormFieldPriExtend, FormFieldResourceExtend } from '@/components/Extensions'

const { Form, FormItem, useForm } = createForm<ICreateJobFormSchema>()

const useStyles = createUseStyles({
    row3: {
        display: 'grid',
        gap: 40,
        gridTemplateColumns: '280px 300px 280px',
        gridTemplateRows: 'minmax(0px, max-content)',
    },
    row4: {
        display: 'grid',
        columnGap: 40,
        gridTemplateColumns: '120px 120px 480px 100px',
    },
    resource: {
        gridColumnStart: 3,
        display: 'grid',
        columnGap: 40,
        gridTemplateColumns: '260px 120px 40px',
    },
})

export interface IJobFormProps {
    job?: IJobSchema
    onSubmit: (data: ICreateJobSchema) => Promise<void>
}

export default function JobForm({ job, onSubmit }: IJobFormProps) {
    const EventEmitter = useEventEmitter<{ changes: Partial<ICreateJobFormSchema>; values: ICreateJobFormSchema }>()
    const styles = useStyles()
    const [values, setValues] = useState<ICreateJobFormSchema | undefined>(undefined)
    const [modelTree, setModelTree] = useState<IModelTreeSchema[]>([])
    const [resource, setResource] = React.useState<any>()
    const [t] = useTranslation()
    const [, forceUpdate] = useReducer((x) => x + 1, 0)
    // const [resourcePool, setResourcePool] = React.useState<ISystemResourcePool | undefined>()

    const { query } = useQueryArgs()
    const [form] = useForm()
    const history = useHistory()

    const modelVersionHandler = form.getFieldValue('modelVersionHandler')
    const stepSpecOverWrites = form.getFieldValue('stepSpecOverWrites') as string
    const _modelVersionId = form.getFieldValue('modelVersionUrl')

    const [loading, setLoading] = useState(false)

    const modelVersion: IModelVersionSchema | undefined = React.useMemo(() => {
        if (!modelTree || !_modelVersionId) return undefined
        let version: IModelVersionSchema | undefined
        modelTree?.forEach((v) =>
            v.versions.forEach((versionTmp) => {
                if (versionTmp.versionName === _modelVersionId) {
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
                resourcePool: values_.resourcePoolTmp || values_.resourcePool,
                stepSpecOverWrites: values_.stepSpecOverWrites,
                runtimeVersionUrl: values_.runtimeType === RuntimeType.BUILTIN ? '' : values_.runtimeVersionUrl,
                modelVersionUrl: values_.modelVersionUrl,
                devMode: values_.devMode,
                devPassword: values_.devPassword,
                timeToLiveInSec: values_.timeToLiveInSec,
            }
            console.log(tmp)
            return
            if (values_.rawType && !checkStepSource(values_.stepSpecOverWrites)) return
            try {
                await onSubmit(tmp)
                history.goBack()
            } finally {
                setLoading(false)
            }
        },
        [onSubmit, history, checkStepSource]
    )

    const handleValuesChange = useCallback(
        (_changes: Partial<ICreateJobFormSchema>, values_: ICreateJobFormSchema) => {
            EventEmitter.emit({
                changes: _changes,
                values: values_,
            })
            if ('devMode' in _changes && _changes.devMode) {
                form.setFieldsValue({
                    devPassword: generatePassword(),
                })
            }
            setValues({
                ...values_,
            })
        },
        [EventEmitter, form]
    )

    const sharedFormProps = {
        form,
        FormItem,
        EventEmitter,
        forceUpdate,
    }

    const getRuntimeProps = () => {
        let builtInRuntime
        let runtimeType: RuntimeType = RuntimeType.OTHER
        if (
            query.runtimeVersionUrl &&
            modelVersion?.builtInRuntime &&
            query.runtimeVersionUrl === modelVersion?.builtInRuntime
        ) {
            return {
                builtInRuntime: modelVersion?.builtInRuntime,
                runtimeType: RuntimeType.BUILTIN,
            }
        }
        if (query.runtimeVersionUrl && !modelVersion?.builtInRuntime) {
            return {
                builtInRuntime: query?.runtimeVersionUrl,
                runtimeType: '',
            }
        }
        if (modelVersion?.builtInRuntime) {
            builtInRuntime = modelVersion?.builtInRuntime
            runtimeType = RuntimeType.BUILTIN
        }
        return {
            builtInRuntime,
            runtimeType,
        }
    }

    const getModelProps = () => {
        // eslint-disable-next-line
        let { modelId: _modelId, modelVersionUrl: _modelVersionUrl, modelVersionHandler: _modelVersionHandler } = query
        if (modelTree && _modelId) {
            modelTree?.forEach((v) => {
                if (v.modelId === _modelId) {
                    _modelVersionUrl = v.versions?.[0]?.id
                }
            })
        }
        return {
            modelVersionUrl: _modelVersionUrl,
            modelVersionHandler: _modelVersionHandler,
            setModelTree,
            fullStepSource,
            stepSource,
        }
    }

    const getDatasetProps = () => {
        const { datasetVersionUrls } = query
        return {
            datasetVersionUrls,
        }
    }

    const getResourcePoolProps = () => {
        return {
            resource,
            setResource,
        }
    }

    const isModifiedDataset = React.useMemo(() => {
        return stepSource?.some((v) => v.require_dataset === null || v.require_dataset)
    }, [stepSource])

    console.log(resource, form.getFieldsValue())

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
            {isModifiedDataset && <FormFieldDataset {...sharedFormProps} {...getDatasetProps()} />}
            {/* runtime config */}
            <Divider orientation='top'>{t('Runtime')}</Divider>
            <FormFieldRuntime {...sharedFormProps} {...getRuntimeProps()} />
            {/* advanced config */}
            <Divider orientation='top'>{t('job.advanced')}</Divider>
            {/* debug config */}
            <FormFieldDevMode {...sharedFormProps} />
            {/* auto release time config */}
            <FormFieldAutoRelease {...sharedFormProps} />
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

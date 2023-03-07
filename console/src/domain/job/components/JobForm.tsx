import React, { useCallback, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { isModified } from '@/utils'
import ModelSelector from '@/domain/model/components/ModelSelector'
import Divider from '@/components/Divider'
import ModelVersionSelector, { IDataSelectorRef } from '@/domain/model/components/ModelVersionSelector'
import DatasetSelector from '@/domain/dataset/components/DatasetSelector'
import DatasetVersionSelector from '@/domain/dataset/components/DatasetVersionSelector'
import Input, { NumberInput } from '@starwhale/ui/Input'
import _ from 'lodash'
import RuntimeVersionSelector from '@/domain/runtime/components/RuntimeVersionSelector'
import RuntimeSelector from '@/domain/runtime/components/RuntimeSelector'
import ResourcePoolSelector from '@/domain/setting/components/ResourcePoolSelector'
import { IModelVersionSchema, StepSpec } from '@/domain/model/schemas/modelVersion'
import Editor from '@monaco-editor/react'
import yaml from 'js-yaml'
import { createUseStyles } from 'react-jss'
import { toaster } from 'baseui/toast'
import IconFont from '@starwhale/ui/IconFont'
import Button from '@starwhale/ui/Button'
import ResourceSelector from '@/domain/setting/components/ResourceSelector'
import { min, max } from '@/components/Form/validators'
import { ISystemResourcePool } from '@/domain/setting/schemas/system'

import { ICreateJobFormSchema, ICreateJobSchema, IJobFormSchema } from '../schemas/job'
import { Toggle } from '@starwhale/ui/Select'
import { usePage } from '@/hooks/usePage'
import { useFetchDatasetVersionsByIds } from '@/domain/dataset/hooks/useFetchDatasetVersions'
import MultiTags from '@/components/Tag/MultiTags'
import { formatTimestampDateTime } from '@/utils/datetime'

const { Form, FormItem, useForm } = createForm<ICreateJobFormSchema>()

const useStyles = createUseStyles({
    row3: {
        display: 'grid',
        gap: 40,
        gridTemplateColumns: '280px 300px 280px',
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
    job?: IJobFormSchema
    onSubmit: (data: ICreateJobSchema) => Promise<void>
}

export default function JobForm({ job, onSubmit }: IJobFormProps) {
    const styles = useStyles()
    const [values, setValues] = useState<ICreateJobFormSchema | undefined>(undefined)
    const { projectId } = useParams<{ projectId: string }>()
    const [modelId, setModelId] = useState('')
    const [datasetId, setDatasetId] = useState('')
    const [runtimeId, setRuntimeId] = useState('')
    const [modelVersionId, setModelVersionId] = useState('')
    const [rawType, setRawType] = React.useState(false)
    const [stepSpecOverWrites, setStepSpecOverWrites] = React.useState('')
    const [t] = useTranslation()
    const [resourcePool, setResourcePool] = React.useState<ISystemResourcePool | undefined>()

    const [datasetVersionsByIds, setDatasetVersionIds] = useState('')
    const [page] = usePage()
    const [form] = useForm()
    const history = useHistory()

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useCallback(
        (_changes, values_) => {
            // console.log(_changes, values_)
            if (values_.modelId) setModelId(values_.modelId)
            if (values_.datasetId) setDatasetId(values_.datasetId)
            if (values_.runtimeId) setRuntimeId(values_.runtimeId)
            if (values_.modelVersionUrl) setModelVersionId(values_.modelVersionUrl)
            let rawTypeTmp = values_.rawType
            // cascade type with default value
            if ('stepSpecOverWrites' in _changes) {
                _changes.stepSpecOverWrites?.forEach((obj: any, i: number) => {
                    obj.resources?.forEach((resource: any, j: number) => {
                        if (!('num' in resource)) {
                            const config = resourcePool?.resources.find((v) => v.name === resource.type)
                            const step = [...values_.stepSpecOverWrites]
                            step[i].resources[j].num = config?.defaults
                            form.setFieldsValue({
                                stepSpecOverWrites: step,
                            })
                        }
                    })
                })
            }
            if ('rawType' in _changes && !_changes.rawType) {
                try {
                    yaml.load(stepSpecOverWrites)
                    rawTypeTmp = false
                } catch (e) {
                    toaster.negative(t('wrong yaml syntax'), { autoHideDuration: 1000 })
                    form.setFieldsValue({
                        rawType: true,
                    })
                    rawTypeTmp = true
                }
            }
            setRawType(rawTypeTmp)
            setValues({
                ...values_,
                rawType: rawTypeTmp,
            })
        },
        [stepSpecOverWrites, form, t, resourcePool]
    )

    const modelVersionRef = React.useRef<IDataSelectorRef<IModelVersionSchema>>(null)
    const modelVersionApi = modelVersionRef.current?.getData()

    const stepSource: StepSpec[] = React.useMemo(() => {
        if (!modelVersionApi) return []
        const list = modelVersionApi?.data?.list ?? []
        return list?.find((v) => v.id === modelVersionId)?.stepSpecs ?? []
    }, [modelVersionApi, modelVersionId])

    const handleFinish = useCallback(
        async (values_: ICreateJobFormSchema) => {
            setLoading(true)
            try {
                yaml.load(stepSpecOverWrites)
            } catch (e) {
                toaster.negative(t('wrong yaml syntax'), { autoHideDuration: 1000 })
                throw e
            }
            try {
                await onSubmit({
                    ..._.omit(values_, [
                        'modelId',
                        'datasetId',
                        'datasetVersionId',
                        'datasetVersionIdsArr',
                        'runtimeId',
                        'rawType',
                        'stepSpecOverWrites',
                    ]),
                    datasetVersionUrls: values_.datasetVersionIdsArr?.join(','),
                    // datasetVersionUrls: values_.datasetVersionId,
                    stepSpecOverWrites: values_.rawType
                        ? stepSpecOverWrites
                        : yaml.dump(_.merge([], stepSource, values_?.stepSpecOverWrites)),
                })
                history.goBack()
            } finally {
                setLoading(false)
            }
        },
        [onSubmit, history, stepSpecOverWrites, t, stepSource]
    )

    const updateFormStepObj = ($newStep: StepSpec[]) => {
        form.setFieldsValue({ stepSpecOverWrites: $newStep })
        setValues({
            ...(values as any),
            stepSpecOverWrites: $newStep,
        })
    }

    const rawRef = React.useRef(false)
    React.useEffect(() => {
        if (rawRef.current === rawType) return
        if (!rawType) {
            updateFormStepObj(yaml.load(stepSpecOverWrites) as StepSpec[])
        } else {
            setStepSpecOverWrites(yaml.dump(_.merge([], stepSource, form.getFieldValue('stepSpecOverWrites'))))
        }
        rawRef.current = rawType
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [stepSource, form, setStepSpecOverWrites, rawType, modelVersionId, stepSpecOverWrites])

    React.useEffect(() => {
        setStepSpecOverWrites(yaml.dump(stepSource))
        updateFormStepObj([...stepSource])
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [stepSource, form, setStepSpecOverWrites])

    const handleAddDataset = useCallback(() => {
        const datasetVersionId = form.getFieldValue('datasetVersionId') as string
        if (!datasetVersionId) return
        const datasetVersionIdsArr = (form.getFieldValue('datasetVersionIdsArr') ?? []) as Array<string>
        const ids = new Set(datasetVersionIdsArr).add(datasetVersionId)
        form.setFieldsValue({
            datasetVersionIdsArr: Array.from(ids),
        })
        setDatasetVersionIds(Array.from(ids).join(','))
    }, [form])

    const datasetsInfo = useFetchDatasetVersionsByIds(projectId, datasetVersionsByIds, page)

    const getValueLabel = useCallback(
        (args) => {
            const dataset = datasetsInfo.data?.list?.find(({ version }) => version?.id === args.option.id)
            // return [dataset?.version?.id, dataset?.version?.name].join('-')
            const item = dataset?.version
            if (!item) return ''

            return [
                item.alias ?? '',
                item.name ? item.name.substring(0, 8) : '',
                item.createdTime ? formatTimestampDateTime(item.createdTime) : '',
            ].join(' : ')
        },
        [datasetsInfo]
    )

    const stepSpecOverWritesList: StepSpec[] = React.useMemo(() => {
        if (!modelVersionApi) return []
        return _.merge([], stepSource, values?.stepSpecOverWrites)
    }, [stepSource, values?.stepSpecOverWrites, modelVersionApi])

    const handleEditorChange = React.useCallback(
        (value: string) => {
            setStepSpecOverWrites(value)
        },
        [setStepSpecOverWrites]
    )

    const getResourceAttr = (i: number, j: number) => {
        const type = form.getFieldValue(['stepSpecOverWrites', i, 'resources', j, 'type'])
        const resource = resourcePool?.resources.find((v) => v.name === type)
        return resource
    }

    return (
        <Form form={form} initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            <Divider orientation='top'>{t('Environment')}</Divider>
            <div className={styles.row3}>
                <FormItem label={t('Resource Pool')} name='resourcePool' required>
                    <ResourcePoolSelector onChangeItem={setResourcePool} autoSelected />
                </FormItem>
            </div>
            <Divider orientation='top'>{t('Model Information')}</Divider>
            <div className={styles.row3}>
                <FormItem label={t('sth name', [t('Model')])} name='modelId' required>
                    <ModelSelector projectId={projectId} />
                </FormItem>
                {modelId && (
                    <FormItem label={t('Version')} required name='modelVersionUrl'>
                        <ModelVersionSelector
                            ref={modelVersionRef}
                            projectId={projectId}
                            modelId={modelId}
                            autoSelected
                        />
                    </FormItem>
                )}
                <FormItem label={t('Raw Type')} name='rawType'>
                    <Toggle />
                </FormItem>
            </div>
            {stepSpecOverWritesList?.length > 0 &&
                !rawType &&
                stepSpecOverWritesList?.map((spec, i) => {
                    return (
                        <div key={[spec?.step_name, i].join('')} className={styles.row4}>
                            <FormItem
                                label={i === 0 && t('Step')}
                                name={['stepSpecOverWrites', i, 'step_name']}
                                required
                            >
                                <Input disabled />
                            </FormItem>
                            <FormItem
                                label={i === 0 && t('Task Amount')}
                                name={['stepSpecOverWrites', i, 'task_num']}
                                required
                            >
                                <NumberInput />
                            </FormItem>
                            {spec.resources && spec.resources?.length > 0 && (
                                <div>
                                    {spec.resources?.map((resource, j) => (
                                        <div
                                            className={styles.resource}
                                            key={['stepSpecOverWrites', i, 'resources', j].join('')}
                                        >
                                            <FormItem
                                                label={i === 0 && j === 0 && t('Resource')}
                                                name={['stepSpecOverWrites', i, 'resources', j, 'type']}
                                                deps={['resourcePool']}
                                            >
                                                <ResourceSelector data={resourcePool?.resources ?? []} />
                                            </FormItem>
                                            <FormItem
                                                label={i === 0 && j === 0 && t('Resource Amount')}
                                                name={['stepSpecOverWrites', i, 'resources', j, 'num']}
                                                deps={['stepSpecOverWrites']}
                                                validators={[
                                                    getResourceAttr(i, j)?.min
                                                        ? min(
                                                              (getResourceAttr(i, j)?.min as number) - 1,
                                                              `should be between ${getResourceAttr(i, j)?.min} - ${
                                                                  getResourceAttr(i, j)?.max
                                                              }`
                                                          )
                                                        : null,
                                                    getResourceAttr(i, j)?.max
                                                        ? max(
                                                              (getResourceAttr(i, j)?.max as number) + 1,
                                                              `should be between ${getResourceAttr(i, j)?.min} - ${
                                                                  getResourceAttr(i, j)?.max
                                                              }`
                                                          )
                                                        : null,
                                                ]}
                                            >
                                                <NumberInput />
                                            </FormItem>
                                            <FormItem label={i === 0 && j === 0 && ' '}>
                                                <Button
                                                    as='link'
                                                    type='button'
                                                    onClick={() => {
                                                        const $step = [...stepSpecOverWritesList]
                                                        $step?.[i]?.resources?.splice(j, 1)
                                                        updateFormStepObj($step)
                                                    }}
                                                    startEnhancer={
                                                        <IconFont
                                                            type='item-reduce'
                                                            style={{ color: 'red', marginTop: '8px' }}
                                                            size={20}
                                                        />
                                                    }
                                                />
                                            </FormItem>
                                        </div>
                                    ))}
                                </div>
                            )}
                            <FormItem label={i === 0 && t('Add Resource')}>
                                <Button
                                    size='compact'
                                    type='button'
                                    onClick={() => {
                                        const $step = [...stepSpecOverWritesList]
                                        $step?.[i]?.resources?.push({})
                                        updateFormStepObj($step)
                                    }}
                                    startEnhancer={<IconFont type='add' kind='white' />}
                                >
                                    Add
                                </Button>
                            </FormItem>
                        </div>
                    )
                })}
            <div
                style={{
                    display: rawType ? 'block' : 'none',
                }}
            >
                <Editor
                    height='500px'
                    width='960px'
                    defaultLanguage='yaml'
                    value={stepSpecOverWrites}
                    theme='vs-dark'
                    // @ts-ignore
                    onChange={handleEditorChange}
                />
            </div>

            <Divider orientation='top'>{t('Datasets')}</Divider>
            <div className={styles.row3}>
                <FormItem label={t('sth name', [t('Dataset')])} name='datasetId'>
                    <DatasetSelector projectId={projectId} />
                </FormItem>
                {datasetId && (
                    <FormItem label={t('Version')} name='datasetVersionId'>
                        <DatasetVersionSelector projectId={projectId} datasetId={datasetId} autoSelected />
                    </FormItem>
                )}
                <div className='fac'>
                    <Button
                        size='compact'
                        type='button'
                        onClick={handleAddDataset}
                        startEnhancer={<IconFont type='add' kind='white' />}
                    >
                        Add
                    </Button>
                </div>
            </div>
            <div className='bfc' style={{ width: '280px', marginBottom: '36px' }}>
                <FormItem label={t('Selected Dataset')} name='datasetVersionIdsArr' required>
                    <MultiTags placeholder='' getValueLabel={getValueLabel} />
                </FormItem>
            </div>
            <Divider orientation='top'>{t('Runtime')}</Divider>
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
            <FormItem>
                <div style={{ display: 'flex', gap: 20, marginTop: 60 }}>
                    <div style={{ flexGrow: 1 }} />
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

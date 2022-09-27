import React, { useCallback, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE, KIND } from 'baseui/button'
import { isModified } from '@/utils'
import ModelSelector from '@/domain/model/components/ModelSelector'
import Divider from '@/components/Divider'
import ModelVersionSelector, { IDataSelectorRef } from '@/domain/model/components/ModelVersionSelector'
import DatasetSelector from '@/domain/dataset/components/DatasetSelector'
import DatasetVersionSelector from '@/domain/dataset/components/DatasetVersionSelector'
import NumberInput from '@/components/Input/NumberInput'
import _ from 'lodash'
import RuntimeVersionSelector from '@/domain/runtime/components/RuntimeVersionSelector'
import RuntimeSelector from '@/domain/runtime/components/RuntimeSelector'
import DeviceSelector from '@/domain/setting/components/DeviceSelector'
import ResourcePoolSelector from '@job/components/ResourcePoolSelector'
import { ICreateJobFormSchema, ICreateJobSchema, IJobFormSchema } from '../schemas/job'
import { IModelVersionSchema } from '@/domain/model/schemas/modelVersion'
import Input from '@/components/Input'
import { Textarea } from 'baseui/textarea'
import Editor, { DiffEditor, useMonaco, loader } from '@monaco-editor/react'
import yaml from 'js-yaml'
import Toggle from '@/components/Select/Toggle'

const { Form, FormItem, useForm } = createForm<ICreateJobFormSchema>()

export interface IJobFormProps {
    job?: IJobFormSchema
    onSubmit: (data: ICreateJobSchema) => Promise<void>
}

const SELELCTORWIDTH = '300px'

export default function JobForm({ job, onSubmit }: IJobFormProps) {
    const [values, setValues] = useState<ICreateJobFormSchema | undefined>(undefined)
    const { projectId } = useParams<{ projectId: string }>()
    const [modelId, setModelId] = useState('')
    const [datasetId, setDatasetId] = useState('')
    const [runtimeId, setRuntimeId] = useState('')
    const [modelVersionId, setModelVersionId] = useState('')
    const [rawType, setRawType] = React.useState(false)

    // const [datasetVersionsByIds, setDatasetVersionIds] = useState('')
    // const [page] = usePage()
    const [form] = useForm()
    const history = useHistory()

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useCallback((_changes, values_, ...rest) => {
        console.log(_changes, values_)
        setValues(values_)
        if (values_.modelId) setModelId(values_.modelId)
        if (values_.datasetId) setDatasetId(values_.datasetId)
        if (values_.runtimeId) setRuntimeId(values_.runtimeId)
        if (values_.modelVersionUrl) setModelVersionId(values_.modelVersionUrl)
        setRawType(values_.rawType)
    }, [])

    const handleFinish = useCallback(
        async (values_: ICreateJobFormSchema) => {
            setLoading(true)
            try {
                await onSubmit({
                    ..._.omit(values_, [
                        'modelId',
                        'datasetId',
                        'datasetVersionId',
                        'datasetVersionIdsArr',
                        'runtimeId',
                        'rawType',
                    ]),
                    // datasetVersionUrls: values_.datasetVersionIdsArr?.join(','),
                    datasetVersionUrls: values_.datasetVersionId,
                })
                history.goBack()
            } finally {
                setLoading(false)
            }
        },
        [onSubmit, history]
    )

    // const handleAddDataset = useCallback(() => {
    //     const datasetVersionId = form.getFieldValue('datasetVersionId') as string
    //     if (!datasetVersionId) return
    //     const datasetVersionIdsArr = (form.getFieldValue('datasetVersionIdsArr') ?? []) as Array<string>
    //     const ids = new Set(datasetVersionIdsArr).add(datasetVersionId)
    //     form.setFieldsValue({
    //         datasetVersionIdsArr: Array.from(ids),
    //     })
    //     setDatasetVersionIds(Array.from(ids).join(','))
    // }, [form])

    // const datasetsInfo = useFetchDatasetVersionsByIds(projectId, datasetVersionsByIds, page)

    const [t] = useTranslation()

    // const getValueLabel = useCallback(
    //     (args) => {
    //         const dataset = datasetsInfo.data?.list?.find(({ version }) => version?.id === args.option.id)
    //         return [dataset?.version?.id, dataset?.version?.name].join('-')
    //     },
    //     [datasetsInfo]
    // )

    const modelVersionRef = React.useRef<IDataSelectorRef<IModelVersionSchema>>(null)

    const stepSpecs = React.useMemo(() => {
        if (!modelVersionRef.current) return []
        const list = modelVersionRef.current.getDataList()
        const $step = list?.find((v) => v.id === modelVersionId)?.stepSpecs ?? []
        return _.merge($step, values?.stepSpecOverWrites)
    }, [modelVersionRef, modelVersionId, values])

    return (
        <Form form={form} initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            <Divider orientation='top'>{t('Model Information')}</Divider>
            <div className='bfc' style={{ display: 'flex', alignItems: 'left', gap: 40 }}>
                <FormItem label={t('sth name', [t('Model')])} name='modelId' required>
                    <ModelSelector
                        projectId={projectId}
                        overrides={{
                            Root: {
                                style: {
                                    width: SELELCTORWIDTH,
                                },
                            },
                        }}
                    />
                </FormItem>
                {modelId && (
                    <FormItem key={modelId} label={t('Version')} required name='modelVersionUrl'>
                        <ModelVersionSelector
                            ref={modelVersionRef}
                            projectId={projectId}
                            modelId={modelId}
                            autoSelected
                            overrides={{
                                Root: {
                                    style: {
                                        width: SELELCTORWIDTH,
                                    },
                                },
                            }}
                        />
                    </FormItem>
                )}
                <FormItem label={t('Raw Type')} name='rawType'>
                    <Toggle />
                </FormItem>
            </div>
            {stepSpecs.length > 0 &&
                !rawType &&
                stepSpecs.map((spec, i) => {
                    return (
                        <div
                            key={spec.step_name ?? i}
                            style={{
                                display: 'grid',
                                gap: 40,
                                gridTemplateColumns: '280px 140px 280px 140px',
                            }}
                        >
                            <FormItem
                                label={i === 0 && t('Step')}
                                name={['stepSpecOverWrites', i, 'step_name']}
                                required
                                initialValue={spec?.step_name}
                            >
                                <Input disabled />
                            </FormItem>
                            <FormItem
                                label={i === 0 && t('Task Amount')}
                                name={['stepSpecOverWrites', i, 'task_num']}
                                required
                                initialValue={spec.task_num}
                            >
                                <NumberInput disabled={!spec?.overwriteable} />
                            </FormItem>
                            {spec.resources?.map((resource, j) => (
                                <>
                                    <FormItem
                                        label={i === 0 && t('Resource')}
                                        name={['stepSpecOverWrites', i, 'resources', j, 'type']}
                                        required
                                        initialValue={resource.type}
                                    >
                                        <DeviceSelector disabled={!spec?.overwriteable} />
                                    </FormItem>
                                    <FormItem
                                        label={i === 0 && t('Resource Amount')}
                                        name={['stepSpecOverWrites', i, 'resources', j, 'num']}
                                        required
                                        initialValue={resource.num}
                                    >
                                        <NumberInput disabled={!spec?.overwriteable} />
                                    </FormItem>
                                </>
                            ))}
                        </div>
                    )
                })}

            {rawType && (
                <Editor
                    height='500px'
                    width='920px'
                    defaultLanguage='yaml'
                    defaultValue={yaml.dump(stepSpecs)}
                    theme='vs-dark'
                />
            )}
            {/* <Textarea value={JSON.stringify(stepSpecs)}></Textarea> */}
            <Divider orientation='top'>{t('Datasets')}</Divider>
            <div style={{ display: 'flex', alignItems: 'left', columnGap: 40, flexWrap: 'wrap' }}>
                <FormItem label={t('sth name', [t('Dataset')])} name='datasetId'>
                    <DatasetSelector
                        projectId={projectId}
                        overrides={{
                            Root: {
                                style: {
                                    width: SELELCTORWIDTH,
                                },
                            },
                        }}
                    />
                </FormItem>
                {datasetId && (
                    <FormItem key={datasetId} label={t('Version')} name='datasetVersionId'>
                        <DatasetVersionSelector
                            projectId={projectId}
                            datasetId={datasetId}
                            autoSelected
                            overrides={{
                                Root: {
                                    style: {
                                        width: SELELCTORWIDTH,
                                    },
                                },
                            }}
                        />
                    </FormItem>
                )}
                {/* <div className='fac'>
                    <Button
                        size='compact'
                        type='button'
                        onClick={handleAddDataset}
                        startEnhancer={<IconFont type='add' kind='white' />}
                    >
                        Add
                    </Button>
                </div> */}
            </div>
            {/* <div className='bfc' style={{ width: '280px', marginBottom: '36px' }}>
                <FormItem label={t('Selected Dataset')} name='datasetVersionIdsArr' required>
                    <MultiTags placeholder='' getValueLabel={getValueLabel} />
                </FormItem>
            </div> */}
            <Divider orientation='top'>{t('Runtime')}</Divider>
            <div style={{ display: 'flex', alignItems: 'left', gap: 40, flexWrap: 'wrap', marginBottom: '36px' }}>
                <FormItem label={t('Runtime')} name='runtimeId' required>
                    <RuntimeSelector
                        projectId={projectId}
                        overrides={{
                            Root: {
                                style: {
                                    width: SELELCTORWIDTH,
                                },
                            },
                        }}
                    />
                </FormItem>
                {runtimeId && (
                    <FormItem key={runtimeId} label={t('Version')} required name='runtimeVersionUrl'>
                        <RuntimeVersionSelector
                            projectId={projectId}
                            runtimeId={runtimeId}
                            autoSelected
                            overrides={{
                                Root: {
                                    style: {
                                        width: SELELCTORWIDTH,
                                    },
                                },
                            }}
                        />
                    </FormItem>
                )}
            </div>
            <Divider orientation='top'>{t('Environment')}</Divider>
            <div style={{ display: 'flex', alignItems: 'left', gap: 40, flexWrap: 'wrap', marginBottom: '36px' }}>
                <FormItem label={t('Resource')} name='device' required>
                    <DeviceSelector
                        overrides={{
                            Root: {
                                style: {
                                    width: SELELCTORWIDTH,
                                },
                            },
                        }}
                    />
                </FormItem>
                <FormItem label={t('Resource Amount')} name='deviceAmount' required>
                    <NumberInput
                        overrides={{
                            Root: {
                                style: {
                                    width: SELELCTORWIDTH,
                                },
                            },
                        }}
                    />
                </FormItem>
                <FormItem label={t('Resource Pool')} name='resourcePool' required initialValue='default'>
                    <ResourcePoolSelector />
                </FormItem>
            </div>

            <FormItem>
                <div style={{ display: 'flex', gap: 20, marginTop: 60 }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button
                        size={SIZE.compact}
                        kind={KIND.secondary}
                        type='button'
                        onClick={() => {
                            history.goBack()
                        }}
                    >
                        {t('cancel')}
                    </Button>
                    <Button size={SIZE.compact} isLoading={loading} disabled={!isModified(job, values)}>
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}

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
import { IModelVersionSchema, StepSpec } from '@/domain/model/schemas/modelVersion'
import Input from '@/components/Input'
import Editor from '@monaco-editor/react'
import yaml from 'js-yaml'
import Toggle from '@/components/Select/Toggle'
import { createUseStyles } from 'react-jss'
import { toaster } from 'baseui/toast'
import { ICreateJobFormSchema, ICreateJobSchema, IJobFormSchema } from '../schemas/job'

const { Form, FormItem, useForm } = createForm<ICreateJobFormSchema>()

const useStyles = createUseStyles({
    row3: {
        display: 'grid',
        gap: 40,
        gridTemplateColumns: '280px 300px 280px',
    },
    row4: {
        display: 'grid',
        gap: 40,
        gridTemplateColumns: '280px 140px 280px 140px',
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

    // const [datasetVersionsByIds, setDatasetVersionIds] = useState('')
    // const [page] = usePage()
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
        [stepSpecOverWrites, form, t]
    )

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
                    // datasetVersionUrls: values_.datasetVersionIdsArr?.join(','),
                    datasetVersionUrls: values_.datasetVersionId,
                    stepSpecOverWrites: values_.rawType ? stepSpecOverWrites : yaml.dump(values_.stepSpecOverWrites),
                })
                history.goBack()
            } finally {
                setLoading(false)
            }
        },
        [onSubmit, history, stepSpecOverWrites, t]
    )
    const modelVersionRef = React.useRef<IDataSelectorRef<IModelVersionSchema>>(null)
    const modelVersionApi = modelVersionRef.current?.getData()

    const stepSource = React.useMemo(() => {
        if (!modelVersionApi) return []
        const list = modelVersionApi?.data?.list ?? []
        return list?.find((v) => v.id === modelVersionId)?.stepSpecs ?? []
    }, [modelVersionApi, modelVersionId])

    const rawRef = React.useRef(false)
    React.useEffect(() => {
        if (rawRef.current === rawType) return
        if (!rawType) {
            const $stepOverided = yaml.load(stepSpecOverWrites)
            form.setFieldsValue({ stepSpecOverWrites: $stepOverided as StepSpec[] })
        } else {
            const $stepOverided = _.merge([], stepSource, values?.stepSpecOverWrites)
            setStepSpecOverWrites(yaml.dump($stepOverided))
        }
        rawRef.current = rawType
    }, [
        stepSource,
        form,
        setStepSpecOverWrites,
        rawType,
        modelVersionId,
        stepSpecOverWrites,
        values?.stepSpecOverWrites,
    ])

    React.useEffect(() => {
        setStepSpecOverWrites(yaml.dump(stepSource))
        form.setFieldsValue({ stepSpecOverWrites: [...stepSource] })
    }, [stepSource, form, setStepSpecOverWrites])

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

    // const getValueLabel = useCallback(
    //     (args) => {
    //         const dataset = datasetsInfo.data?.list?.find(({ version }) => version?.id === args.option.id)
    //         return [dataset?.version?.id, dataset?.version?.name].join('-')
    //     },
    //     [datasetsInfo]
    // )

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

    return (
        <Form form={form} initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
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
                                <React.Fragment key={['stepSpecOverWrites', i, 'resources', j].join('')}>
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
                                </React.Fragment>
                            ))}
                        </div>
                    )
                })}

            {rawType && (
                <Editor
                    height='500px'
                    width='960px'
                    defaultLanguage='yaml'
                    value={stepSpecOverWrites}
                    theme='vs-dark'
                    // @ts-ignore
                    onChange={handleEditorChange}
                />
            )}
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
            <Divider orientation='top'>{t('Environment')}</Divider>
            <div className={styles.row3}>
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

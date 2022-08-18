import React, { useCallback, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE, KIND } from 'baseui/button'
import { isModified } from '@/utils'
import ModelSelector from '@/domain/model/components/ModelSelector'
import Divider from '@/components/Divider'
import ModelVersionSelector from '@/domain/model/components/ModelVersionSelector'
import MultiTags from '@/components/Tag/MultiTags'
import DatasetSelector from '@/domain/dataset/components/DatasetSelector'
import DatasetVersionSelector from '@/domain/dataset/components/DatasetVersionSelector'
import NumberInput from '@/components/Input/NumberInput'
import _ from 'lodash'
import { useFetchDatasetVersionsByIds } from '@/domain/dataset/hooks/useFetchDatasetVersions'
import { usePage } from '@/hooks/usePage'
import IconFont from '@/components/IconFont'
import RuntimeVersionSelector from '@/domain/runtime/components/RuntimeVersionSelector'
import RuntimeSelector from '@/domain/runtime/components/RuntimeSelector'
import DeviceSelector from '@/domain/setting/components/DeviceSelector'
import { ICreateJobFormSchema, ICreateJobSchema, IJobFormSchema } from '../schemas/job'

const { Form, FormItem, useForm } = createForm<ICreateJobFormSchema>()

export interface IJobFormProps {
    job?: IJobFormSchema
    onSubmit: (data: ICreateJobSchema) => Promise<void>
}

export default function JobForm({ job, onSubmit }: IJobFormProps) {
    const [values, setValues] = useState<ICreateJobFormSchema | undefined>(undefined)
    const { projectId } = useParams<{ projectId: string }>()
    const [modelId, setModelId] = useState('')
    const [datasetId, setDatasetId] = useState('')
    const [runtimeId, setRuntimeId] = useState('')
    const [datasetVersionsByIds, setDatasetVersionIds] = useState('')
    const [page] = usePage()
    const [form] = useForm()
    const history = useHistory()

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useCallback((_changes, values_) => {
        setValues(values_)
        if (values_.modelId) setModelId(values_.modelId)
        if (values_.datasetId) setDatasetId(values_.datasetId)
        if (values_.runtimeId) setRuntimeId(values_.runtimeId)
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
                    ]),
                    datasetVersionUrls: values_.datasetVersionIdsArr?.join(','),
                })
                history.goBack()
            } finally {
                setLoading(false)
            }
        },
        [onSubmit, history]
    )

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

    const [t] = useTranslation()

    const getValueLabel = useCallback(
        (args) => {
            const dataset = datasetsInfo.data?.list?.find(({ version }) => version?.id === args.option.id)
            return [dataset?.version?.id, dataset?.version?.name].join('-')
        },
        [datasetsInfo]
    )

    return (
        <Form form={form} initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            <Divider orientation='top'>{t('Model Information')}</Divider>
            <div className='bfc' style={{ display: 'flex', alignItems: 'left', gap: 40, marginBottom: '36px' }}>
                <FormItem label={t('sth name', [t('Model')])} name='modelId' required>
                    <ModelSelector
                        projectId={projectId}
                        overrides={{
                            Root: {
                                style: {
                                    width: '280px',
                                },
                            },
                        }}
                    />
                </FormItem>
                {modelId && (
                    <FormItem key={modelId} label={t('Version')} required name='modelVersionUrl'>
                        <ModelVersionSelector
                            projectId={projectId}
                            modelId={modelId}
                            overrides={{
                                Root: {
                                    style: {
                                        width: '280px',
                                    },
                                },
                            }}
                        />
                    </FormItem>
                )}
            </div>
            <Divider orientation='top'>{t('Datasets')}</Divider>
            <div style={{ display: 'flex', alignItems: 'left', columnGap: 40, flexWrap: 'wrap' }}>
                <FormItem label={t('sth name', [t('Dataset')])} name='datasetId'>
                    <DatasetSelector
                        projectId={projectId}
                        overrides={{
                            Root: {
                                style: {
                                    width: '280px',
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
                            overrides={{
                                Root: {
                                    style: {
                                        width: '280px',
                                    },
                                },
                            }}
                        />
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
            <div style={{ display: 'flex', alignItems: 'left', gap: 40, flexWrap: 'wrap', marginBottom: '36px' }}>
                <FormItem label={t('Runtime')} name='runtimeId' required>
                    <RuntimeSelector
                        projectId={projectId}
                        overrides={{
                            Root: {
                                style: {
                                    width: '280px',
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
                            overrides={{
                                Root: {
                                    style: {
                                        width: '280px',
                                    },
                                },
                            }}
                        />
                    </FormItem>
                )}
            </div>
            <Divider orientation='top'>{t('Environment')}</Divider>
            <div style={{ display: 'flex', alignItems: 'left', gap: 40, flexWrap: 'wrap', marginBottom: '36px' }}>
                <FormItem label={t('Device')} name='device' required>
                    <DeviceSelector
                        overrides={{
                            Root: {
                                style: {
                                    width: '280px',
                                },
                            },
                        }}
                    />
                </FormItem>
                <FormItem label={t('Device Amount')} name='deviceAmount' required>
                    <NumberInput
                        overrides={{
                            Root: {
                                style: {
                                    width: '280px',
                                },
                            },
                        }}
                    />
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

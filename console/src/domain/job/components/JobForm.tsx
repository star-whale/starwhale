import React, { useCallback, useState } from 'react'
import { useHistory } from 'react-router-dom'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import { isModified } from '@/utils'
import ModelSelector from '@/domain/model/components/ModelSelector'
import { LabelLarge } from 'baseui/typography'
import Divider from '@/components/Divider'
import { useParams } from 'react-router'
import ModelVersionSelector from '@/domain/model/components/ModelVersionSelector'
import MultiTags from '@/components/Tag/MultiTags'
import DatasetSelector from '@/domain/dataset/components/DatasetSelector'
import DatasetVersionSelector from '@/domain/dataset/components/DatasetVersionSelector'
import BaseImageSelector from '@/domain/runtime/components/BaseImageSelector'
import NumberInput from '@/components/Input/NumberInput'
import _ from 'lodash'
import DeviceSelector from '../../runtime/components/DeviceSelector'
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
    const [, setDatasetVersionIds] = useState('')
    const [form] = useForm()
    const history = useHistory()

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useCallback((_changes, values_) => {
        setValues(values_)
        if (values_.modelId) setModelId(values_.modelId)
        if (values_.datasetId) setDatasetId(values_.datasetId)
    }, [])

    const handleFinish = useCallback(
        async (values_: ICreateJobFormSchema) => {
            setLoading(true)
            try {
                await onSubmit({
                    ..._.omit(values_, ['modelId', 'datasetId', 'datasetVersionId', 'datasetVersionIdsArr']),
                    datasetVersionIds: values_.datasetVersionIdsArr?.join(','),
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
        const ids = new Set(...datasetVersionIdsArr).add(datasetVersionId)
        form.setFieldsValue({
            datasetVersionIdsArr: Array.from(ids),
        })
        setDatasetVersionIds(Array.from(ids).join(','))
    }, [form])

    // TODO: show dataset version info
    // let jobsInfo = useFetchDatasetVersionsByIds(projectId, datasetVersionsByIds, page)

    // useEffect(() => {
    //     if (!datasetVersionsByIds.length) return

    //     console.log(jobsInfo.data)
    // }, [jobsInfo, datasetVersionsByIds])

    const [t] = useTranslation()

    return (
        <Form form={form} initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            <Divider orientation='left'>
                <LabelLarge>{t('Model Information')}</LabelLarge>
            </Divider>
            <div style={{ display: 'flex', alignItems: 'left', gap: 20 }}>
                <FormItem label={t('sth name', [t('Model')])} name='modelId' required>
                    <ModelSelector
                        projectId={projectId}
                        overrides={{
                            Root: {
                                style: {
                                    width: '200px',
                                },
                            },
                        }}
                    />
                </FormItem>
                {modelId && (
                    <FormItem key={modelId} label={t('Version')} required name='modelVersionId'>
                        <ModelVersionSelector
                            projectId={projectId}
                            modelId={modelId}
                            overrides={{
                                Root: {
                                    style: {
                                        width: '400px',
                                    },
                                },
                            }}
                        />
                    </FormItem>
                )}
            </div>
            <Divider orientation='left'>
                <LabelLarge>{t('Datasets')}</LabelLarge>
            </Divider>
            <div style={{ display: 'flex', alignItems: 'left', gap: 20, flexWrap: 'wrap' }}>
                <FormItem label={t('sth name', [t('Dataset')])} name='datasetId'>
                    <DatasetSelector
                        projectId={projectId}
                        overrides={{
                            Root: {
                                style: {
                                    width: '200px',
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
                                        width: '400px',
                                    },
                                },
                            }}
                        />
                    </FormItem>
                )}
                <div style={{ marginTop: 30 }}>
                    <Button type='button' onClick={handleAddDataset}>
                        Add
                    </Button>
                </div>
            </div>
            <div style={{ width: '400px' }}>
                <FormItem label={t('Selected Dataset')} name='datasetVersionIdsArr' required>
                    <MultiTags placeholder='' />
                </FormItem>
            </div>
            <Divider orientation='left'>
                <LabelLarge>{t('Environment')}</LabelLarge>
            </Divider>
            <div style={{ display: 'flex', alignItems: 'left', gap: 20, flexWrap: 'wrap' }}>
                <FormItem label={t('BaseImage')} name='baseImageId'>
                    <BaseImageSelector
                        overrides={{
                            Root: {
                                style: {
                                    width: '400px',
                                },
                            },
                        }}
                    />
                </FormItem>
                <FormItem label={t('Device')} name='deviceId'>
                    <DeviceSelector
                        overrides={{
                            Root: {
                                style: {
                                    width: '200px',
                                },
                            },
                        }}
                    />
                </FormItem>
                <FormItem label={t('Device Count')} name='deviceCount'>
                    <NumberInput
                        overrides={{
                            Root: {
                                style: {
                                    width: '200px',
                                },
                            },
                        }}
                    />
                </FormItem>
                {/* <FormItem label={t('Result Output Path')} name='resultOutputPath'>
                    <Input
                        overrides={{
                            Root: {
                                style: {
                                    width: '200px',
                                },
                            },
                        }}
                    />
                </FormItem> */}
            </div>

            <FormItem>
                <div style={{ display: 'flex', gap: 20 }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button
                        type='button'
                        onClick={() => {
                            history.goBack()
                        }}
                    >
                        {t('cancel')}
                    </Button>
                    <Button isLoading={loading} disabled={!isModified(job, values)}>
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}

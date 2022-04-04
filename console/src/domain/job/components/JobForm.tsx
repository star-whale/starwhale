import { ICreateJobFormSchema, ICreateJobSchema, IJobFormSchema, IJobSchema } from '../schemas/job'
import React, { useCallback, useEffect, useState, useMemo, createRef } from 'react'
import { createForm } from '@/components/Form'
import { Input } from 'baseui/input'
import { Textarea } from 'baseui/textarea'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import { isModified } from '@/utils'
import { RadioGroup, Radio, ALIGN } from 'baseui/radio'
import { Select, TYPE, Value } from 'baseui/select'
import ModelSelector from '@/domain/model/components/ModelSelector'
import { Label1, Label2 } from 'baseui/typography'
import Divider from '@/components/Divider'
import { useParams } from 'react-router'
import ModelVersionSelector from '@/domain/model/components/ModelVersionSelector'
import MultiTags from '@/components/Tag/MultiTags'
import DatasetSelector from '@/domain/dataset/components/DatasetSelector'
import DatasetVersionSelector from '@/domain/dataset/components/DatasetVersionSelector'
import BaseImageSelector from '@/domain/runtime/components/BaseImageSelector'
import DeviceSelector from '../../runtime/components/DeviceSelector'
import useRef from 'react'
import NumberInput from '@/components/NumberInput'

const { Form, FormItem, useForm } = createForm<ICreateJobFormSchema>()

export interface IJobFormProps {
    job?: IJobFormSchema
    onSubmit: (data: ICreateJobFormSchema) => Promise<void>
}

export default function JobForm({ job, onSubmit }: IJobFormProps) {
    const [values, setValues] = useState<ICreateJobFormSchema | undefined>(undefined)
    const { projectId } = useParams<{ projectId: string }>()
    const [modelId, setModelId] = useState('')
    const [datasetId, setDatasetId] = useState('')
    const [form] = useForm()

    useEffect(() => {
        if (!job) {
            return
        }
        // todo job edit
        // setDatasetVersionIds(job.datasetVersionIds)
        // setValues({
        // })
    }, [job])

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useCallback((_changes, values_) => {
        console.log(_changes, values_)
        setValues(values_)
        values_.modelId && setModelId(values_.modelId)
        values_.datasetId && setDatasetId(values_.datasetId)
    }, [])

    const handleFinish = useCallback(
        async (values_) => {
            setLoading(true)
            try {
                await onSubmit({
                    ...values_,
                    datasetVersionIds: values_.datasetVersionIds.join(','),
                })
                history.back()
            } finally {
                setLoading(false)
            }
        },
        [onSubmit]
    )

    const handleAddDataset = useCallback(() => {
        const datasetVersionId = form.getFieldValue('datasetVersionId') as string
        if (!datasetVersionId) return
        const datasetVersionIds = (form.getFieldValue('datasetVersionIds') ?? []) as Array<string>
        const ids = new Set(...datasetVersionIds).add(datasetVersionId)
        form.setFieldsValue({
            datasetVersionIds: Array.from(ids),
        })
    }, [])

    const [t] = useTranslation()

    return (
        <Form form={form} initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            <Divider orientation='left'>
                <Label1>{t('Model Information')}</Label1>
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
                    ></ModelSelector>
                </FormItem>
                {modelId && (
                    <FormItem key={modelId} label={t('Version')} required name='modelVersionId'>
                        <ModelVersionSelector
                            projectId={projectId}
                            modelId={modelId}
                            overrides={{
                                Root: {
                                    style: {
                                        width: '200px',
                                    },
                                },
                            }}
                        ></ModelVersionSelector>
                    </FormItem>
                )}
            </div>
            <Divider orientation='left'>
                <Label1>{t('Datasets')}</Label1>
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
                    ></DatasetSelector>
                </FormItem>
                {datasetId && (
                    <FormItem key={datasetId} label={t('Version')} name='datasetVersionId'>
                        <DatasetVersionSelector
                            projectId={projectId}
                            datasetId={datasetId}
                            overrides={{
                                Root: {
                                    style: {
                                        width: '200px',
                                    },
                                },
                            }}
                        ></DatasetVersionSelector>
                    </FormItem>
                )}
                <div style={{ marginTop: 30 }}>
                    <Button type='button' onClick={handleAddDataset}>
                        Add
                    </Button>
                </div>
            </div>
            <div style={{ width: '420px' }}>
                <FormItem label={t('Selected Dataset')} name='datasetVersionIds' required>
                    <MultiTags
                        // value={datasetVersionIds}
                        placeholder={''}
                        // getValueLabel={(params) => {
                        //     // todo with dataset name
                        //     const id = params.option?.id
                        //     return id + ''
                        // }}
                        // onChange={handleResetDataset}
                    />
                </FormItem>
            </div>
            <Divider orientation='left'>
                <Label1>{t('Environment')}</Label1>
            </Divider>
            <div style={{ display: 'flex', alignItems: 'left', gap: 20, flexWrap: 'wrap' }}>
                <FormItem label={t('BaseImage')} name='baseImageId'>
                    <BaseImageSelector
                        overrides={{
                            Root: {
                                style: {
                                    width: '200px',
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
                <FormItem label={t('Result Output Path')} name='resultOutputPath'>
                    <Input
                        overrides={{
                            Root: {
                                style: {
                                    width: '200px',
                                },
                            },
                        }}
                    />
                </FormItem>
            </div>

            <FormItem>
                <div style={{ display: 'flex', gap: 20 }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button
                        type='button'
                        onClick={() => {
                            history.back()
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

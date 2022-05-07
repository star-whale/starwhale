import React, { useCallback, useEffect, useState } from 'react'
import { createForm } from '@/components/Form'
import { Input } from 'baseui/input'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import { isModified } from '@/utils'
import { RadioGroup, Radio, ALIGN } from 'baseui/radio'
import { ICreateDatasetVersionSchema, IDatasetVersionSchema } from '../schemas/datasetVersion'

const { Form, FormItem } = createForm<ICreateDatasetVersionSchema>()

export interface IDatasetVersionFormProps {
    dataset?: IDatasetVersionSchema
    onSubmit: (data: ICreateDatasetVersionSchema) => Promise<void>
}

export default function DatasetVersionForm({ dataset, onSubmit }: IDatasetVersionFormProps) {
    const [values, setValues] = useState<ICreateDatasetVersionSchema | undefined>(undefined)
    const [importBy, setImportBy] = useState('upload')

    useEffect(() => {
        if (!dataset) {
            return
        }
        setValues({
            datasetName: dataset.name,
        })
    }, [dataset])

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useCallback((_changes, values_) => {
        setValues(values_)
    }, [])

    const handleFinish = useCallback(
        async (values_) => {
            setLoading(true)
            try {
                await onSubmit(values_)
            } finally {
                setLoading(false)
            }
        },
        [onSubmit]
    )

    const [t] = useTranslation()

    return (
        <Form initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            {/* TODO display curret dataset name */}
            {/* <FormItem name='datasetName' label={t('sth name', [t('Dataset Version')])}>
                <Input disabled={dataset !== undefined ? true : undefined} />
            </FormItem> */}
            <div style={{ marginBottom: 20 }}>
                <RadioGroup
                    name='number'
                    align={ALIGN.horizontal}
                    value={importBy}
                    onChange={(e) => setImportBy(e.currentTarget.value)}
                >
                    <Radio value='server'>{t('Import from server')}</Radio>
                    <Radio value='upload'>{t('Upload')}</Radio>
                </RadioGroup>
            </div>
            {importBy === 'server' && (
                <FormItem name='importPath' label={t('Import Path')}>
                    <Input disabled={dataset !== undefined ? true : undefined} />
                </FormItem>
            )}
            {importBy === 'upload' && (
                // TODO beauty file upload plugin
                <FormItem name='zipFile' label={t('Upload')} valuePropName='files'>
                    <Input name='files' disabled={dataset !== undefined ? true : undefined} type='file' />

                    {/* <FileUploader
                    onDrop={(acceptedFiles, rejectedFiles) => {}}
                    // progressAmount is a number from 0 - 100 which indicates the percent of file transfer completed
                /> */}
                </FormItem>
            )}
            <FormItem>
                <div style={{ display: 'flex' }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button
                        isLoading={loading}
                        // size={ButtonSize.compact}
                        disabled={!isModified(dataset, values)}
                    >
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}

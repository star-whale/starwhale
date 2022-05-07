import React, { useCallback, useEffect, useState } from 'react'
import { createForm } from '@/components/Form'
import { Input } from 'baseui/input'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import { isModified } from '@/utils'
import { RadioGroup, Radio, ALIGN } from 'baseui/radio'
import { ICreateModelVersionSchema, IModelVersionSchema } from '../schemas/modelVersion'

const { Form, FormItem } = createForm<ICreateModelVersionSchema>()

export interface IModelVersionFormProps {
    model?: IModelVersionSchema
    onSubmit: (data: ICreateModelVersionSchema) => Promise<void>
}

export default function ModelVersionForm({ model, onSubmit }: IModelVersionFormProps) {
    const [values, setValues] = useState<ICreateModelVersionSchema | undefined>(undefined)
    const [importBy, setImportBy] = useState('upload')

    useEffect(() => {
        if (!model) {
            return
        }
        setValues({
            modelName: model.name,
        })
    }, [model])

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
            {/* //TODO: display curret model name */}
            {/* <FormItem name='modelName' label={t('sth name', [t('Model Version')])}>
                <Input disabled={model !== undefined ? true : undefined} />
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
                    <Input disabled={model !== undefined ? true : undefined} />
                </FormItem>
            )}
            {importBy === 'upload' && (
                // TODO: beauty file upload plugin
                <FormItem name='zipFile' label={t('Upload')} valuePropName='files'>
                    <Input name='files' disabled={model !== undefined ? true : undefined} type='file' />

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
                        disabled={!isModified(model, values)}
                    >
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}

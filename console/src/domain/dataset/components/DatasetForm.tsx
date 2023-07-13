import React, { useCallback, useEffect, useState } from 'react'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { ICreateDatasetFormSchema, IDatasetSchema } from '../schemas/dataset'
import { createUseStyles } from 'react-jss'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import User from '@/domain/user/components/User'
import { useProject } from '@/domain/project/hooks/useProject'
import { Toggle } from '@starwhale/ui/Select'
import { useHistory, useParams } from 'react-router-dom'
import { useQueryArgs } from '@starwhale/core'
import { DraggerUpload } from '@starwhale/ui/Upload'
import Button from '@starwhale/ui/Button'
import _ from 'lodash'
import Shared from '@/components/Shared'
import Input from '@starwhale/ui/Input'

const { Form, FormItem, useForm, FormItemLabel } = createForm<ICreateDatasetFormSchema>()

const useStyles = createUseStyles({
    datasetName: {
        display: 'flex',
        alignContent: 'stretch',
        alignItems: 'flex-start',
        gap: '8px',
    },
    shared: {
        'display': 'flex',
        'alignItems': 'center',
        '& [data-baseweb=form-control-container]': {
            marginBottom: '0 !important',
        },
        '& > div:first-child': {
            marginLeft: '22px',
        },
        'marginBottom': '20px',
    },
    row: {
        display: 'grid',
        gap: 40,
        gridTemplateColumns: '660px',
        gridTemplateRows: 'minmax(0px, max-content)',
        marginBottom: '20px',
    },
    upload: {
        'marginBottom': '20px',
        '& .ant-upload-btn': {
            height: '200px !important',
        },
    },
})

export interface IDatasetFormProps {
    dataset?: IDatasetSchema
    onSubmit: (data: ICreateDatasetFormSchema) => Promise<void>
}

export default function DatasetForm({ dataset, onSubmit }: IDatasetFormProps) {
    const [values, setValues] = useState<ICreateDatasetFormSchema | undefined>(undefined)
    const { projectId } = useParams<{ projectId: string }>()
    const { query } = useQueryArgs()
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    const { project } = useProject()
    const styles = useStyles()
    const [form] = useForm()
    const history = useHistory()

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
                await onSubmit({
                    ..._.omit(values_, ['upload']),
                    ...values_.upload,
                })
            } finally {
                setLoading(false)
            }
        },
        [onSubmit]
    )

    const [t] = useTranslation()

    console.log(values)

    const formResetField = useCallback(
        (key: any, value: any) => {
            form.setFieldsValue({
                [key]: value,
            })

            setValues(
                (prev) =>
                    ({
                        ...prev,
                        [key]: value,
                    } as any)
            )
        },
        [form]
    )

    useEffect(() => {
        if (query.datasetVersionId) {
            formResetField('datasetVersionId', query.datasetVersionId)
        }
    }, [query.datasetVersionId, formResetField])

    // console.log(Object.values(MIMES).join(','))

    console.log(form.getFieldValue('shared'))

    return (
        <Form form={form} initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            <div className={styles.datasetName}>
                <FormItemLabel label={t('dataset.create.owner')}>
                    <div className='flex' style={{ height: '32px', alignItems: 'center', gap: '8px' }}>
                        <User user={currentUser} /> /
                    </div>
                </FormItemLabel>
                <FormItemLabel label={t('Project Name')}>
                    <div className='flex' style={{ height: '32px', alignItems: 'center', gap: '8px' }}>
                        {project?.name} /
                    </div>
                </FormItemLabel>
                <FormItem name='datasetName' label={t('sth name', [t('Dataset')])} style={{ minWidth: 280 }} required>
                    <Input size='compact' />
                </FormItem>
            </div>
            <div className={styles.shared}>
                {t('Shared')}:
                <Shared shared={form.getFieldValue('shared') ? 1 : 0} />
                <FormItem name='shared'>
                    <Toggle />
                </FormItem>
            </div>
            <div className={styles.upload} style={{ overflow: 'auto', maxWidth: 800, width: '100%' }}>
                <FormItem name='upload' label={t('dataset.create.files')} required>
                    <DraggerUpload />
                </FormItem>
            </div>

            <FormItem>
                <div style={{ display: 'flex', gap: 20, marginTop: 20 }}>
                    <Button
                        kind='secondary'
                        type='button'
                        onClick={() => {
                            history.goBack()
                        }}
                    >
                        {t('Cancel')}
                    </Button>
                    <Button isLoading={loading} type='submit'>
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}

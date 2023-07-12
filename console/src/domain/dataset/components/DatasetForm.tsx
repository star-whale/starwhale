import React, { useCallback, useEffect, useState } from 'react'
import { createForm } from '@/components/Form'
import { Input } from 'baseui/input'
import useTranslation from '@/hooks/useTranslation'
import { isModified } from '@/utils'
import { ICreateDatasetSchema, IDatasetSchema } from '../schemas/dataset'
import { createUseStyles } from 'react-jss'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import User from '@/domain/user/components/User'
import { useProject } from '@/domain/project/hooks/useProject'
import { Toggle } from '@starwhale/ui/Select'
import { useHistory, useParams } from 'react-router-dom'
import { useQueryArgs } from '@starwhale/core'
import { MIMES } from '@starwhale/core/dataset'
import { DraggerUpload } from '@starwhale/ui/Upload'
import Button from '@starwhale/ui/Button'
import _ from 'lodash'

const { Form, FormItem, useForm } = createForm<ICreateDatasetSchema>()

const useStyles = createUseStyles({
    datasetName: {
        display: 'flex',
        alignContent: 'stretch',
        alignItems: 'flex-start',
        gap: '5px',
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
    onSubmit: (data: ICreateDatasetSchema) => Promise<void>
}

export default function DatasetForm({ dataset, onSubmit }: IDatasetFormProps) {
    const [values, setValues] = useState<ICreateDatasetSchema | undefined>(undefined)
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

    console.log(Object.values(MIMES).join(','))

    return (
        <Form initialValues={values} onFinish={handleFinish} onValuesChange={handleValuesChange}>
            <div className={styles.datasetName}>
                <div
                    style={{
                        margin: '38px 6px 0px',
                        display: 'flex',
                        gap: '5px',
                    }}
                >
                    <User user={currentUser} /> / {project?.name}
                </div>
                <FormItem name='datasetName' label={t('sth name', [t('Dataset')])} style={{ minWidth: 280 }} required>
                    <Input size='compact' />
                </FormItem>
            </div>
            <div className={styles.row}>
                <FormItem label={t('Shared')} name='shared'>
                    <Toggle />
                </FormItem>
            </div>
            <div className={styles.upload} style={{ overflow: 'auto' }}>
                <FormItem name='upload' label={t('sth name', [t('Dataset')])} style={{ minWidth: 280 }} required>
                    <DraggerUpload />
                </FormItem>
            </div>

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
                    <Button isLoading={loading} type='submit'>
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}

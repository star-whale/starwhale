import React, { useCallback, useEffect, useState } from 'react'
import { createForm } from '@/components/Form'
import { Input } from 'baseui/input'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import { isModified } from '@/utils'
import { ICreateDatasetSchema, IDatasetSchema } from '../schemas/dataset'
import { createUseStyles } from 'react-jss'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import User from '@/domain/user/components/User'
import { useProject } from '@/domain/project/hooks/useProject'
import { Toggle } from '@starwhale/ui/Select'
import DatasetSelector from './DatasetSelector'
import { useParams } from 'react-router-dom'
import { useQueryArgs } from '@starwhale/core'
import { MIMES } from '@starwhale/core/dataset'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'
// import Upload from 'antd/lib/upload'
import Upload from 'antd/es/upload'

const { Dragger } = Upload
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

    const props: UploadProps = {
        action: 'https://www.mocky.io/v2/5cc8019d300000980a055e76',
        onChange(info) {
            console.log(info)

            const { status } = info.file
            if (status !== 'uploading') {
                console.log(info.file, info.fileList)
            }
            if (status === 'done') {
            } else if (status === 'error') {
            }
        },
        onDrop(e) {
            console.log('Dropped files', e.dataTransfer.files)
        },
    }

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

    const [fileList, setFileList] = useState<UploadFile[]>([])

    const handleChange: UploadProps['onChange'] = (info) => {
        let newFileList = [...info.fileList]

        // 1. Limit the number of uploaded files
        // Only to show two recent uploaded files, and old ones will be replaced by the new
        newFileList = newFileList.slice(-2)

        // 2. Read from response and show file link
        newFileList = newFileList.map((file) => {
            if (file.response) {
                // Component will show file.url as link
                file.url = file.response.url
            }
            return file
        })

        setFileList(newFileList)
    }

    const itemRender = (originNode: React.ReactNode, file: UploadFile, currentFileList: UploadFile[]) => {
        console.log(originNode, file, currentFileList)
        return originNode
    }

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
                    <User user={currentUser} /> /
                </div>
                <FormItem key='projectName' name='projectName' label={t('sth name', [t('Project')])} required>
                    <Input size='compact' />
                </FormItem>
                <FormItem name='datasetVersionId' label={t('sth name', [t('Dataset')])} style={{ minWidth: 280 }}>
                    <DatasetSelector projectId={projectId} />
                </FormItem>
            </div>
            <div className={styles.row}>
                <FormItem label={t('Shared')} name='shared'>
                    <Toggle />
                </FormItem>
            </div>
            <div className={styles.upload} style={{ overflow: 'auto' }}>
                {/* accept={Object.values(MIMES).join(',')} */}
                <Dragger
                    {...props}
                    name='file'
                    multiple
                    directory
                    onChange={handleChange}
                    fileList={fileList}
                    itemRender={itemRender}
                >
                    <p className='ant-upload-drag-icon'></p>
                    <p className='ant-upload-text'>Click or drag file to this area to upload</p>
                    <p className='ant-upload-hint'>
                        Support for a single or bulk upload. Strictly prohibited from uploading company data or other
                        banned files.
                    </p>
                </Dragger>
            </div>

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

import React, { useEffect } from 'react'
import { useUpload } from './hooks/useUpload'
import { findMostFrequentType, getSignUrls, getUploadName, pickAttr } from './utils'
import Button from '../Button'
import { createUseStyles } from 'react-jss'
import useTranslation from '@/hooks/useTranslation'
import { getReadableStorageQuantityStr } from '../utils/index'
import Text from '../Text/Text'
import { useDropzone } from './react-dropzone'
import { sign } from '@/domain/base/services/filestore'
import _ from 'lodash'
import useUploadingControl from './hooks/useUploadingControl'
import { UploadFile } from './types'
import { ItemRender } from './UploadItem'

const useStyles = createUseStyles({
    drag: {
        'display': 'flex',
        'alignContent': 'stretch',
        'alignItems': 'flex-start',
        'flexDirection': 'column',
        'gap': '8px',
        '& .ant-upload-drag': {
            backgroundColor: '#FAFBFC !important',
        },
        '& .ant-upload-btn': {
            'height': '170px !important',
            'alignItems': 'center',
            'justifyContent': 'center',
            ':hover': {
                border: '1px solid #5181E0; !important',
            },
        },
        '& .ant-upload-drag-icon': {
            color: 'rgba(2,16,43,0.40) !important',
            marginBottom: '10px !important',
        },
        '& .ant-upload-text': {
            color: 'rgba(2,16,43,0.40) !important',
            marginBottom: '30px !important',
            fontSize: '14px !important',
        },
        '& .ant-upload-action': {
            gap: '20px',
            display: 'flex',
            justifyContent: 'center',
            alignContent: 'center',
        },
        '& .ant-upload-list': {
            maxHeight: '230px',
            width: '100%',
            overflow: 'auto',
        },
        '& .ant-upload-list-item-container': {
            '&:hover': {
                backgroundColor: '#EBF1FF !important',
            },
        },
        '& .ant-upload-list-item': {
            display: 'flex',
            alignItems: 'center',
            marginTop: '0px !important',
            fontSize: '14px !important',
            height: '32px !important',
            borderBottom: '1px solid #EEF1F6',
            paddingRight: '8px',
        },
        '& .ant-upload-list-item-name': {
            flex: 1,
        },
        '& .ant-upload-list-item-size': {
            color: ' rgba(2,16,43,0.40)',
        },
    },
})
const baseStyle = {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '20px',
    borderWidth: 1,
    borderRadius: 2,
    borderColor: '#CFD7E6;',
    borderStyle: 'dashed',
    backgroundColor: '#fafbfc',
    color: '#bdbdbd',
    outline: 'none',
    transition: 'border .24s ease-in-out',
}
const focusedStyle = {
    borderColor: '#5181E0',
}
const acceptStyle = {
    borderColor: '#5181E0',
}
const rejectStyle = {
    borderColor: '#5181E0',
}

type ValueT = { storagePath: string; type: string }
interface IDraggerUploadProps {
    value?: ValueT
    onChange?: (data: ValueT) => void
}

const UPLOAD_MAX = 1000
type UploadControlT = {
    file: UploadFile
    oss: string
    resp: any
}
function DraggerUpload({ onChange }: IDraggerUploadProps) {
    const styles = useStyles()
    const [t] = useTranslation()
    const { getProps, signPrefix, reset } = useUpload()
    const rest = getProps()
    const [fileList, setFileList] = React.useState<UploadFile[]>([])
    const [fileSuccessList, setFileSuccessList] = React.useState<UploadFile[]>([])
    const [fileFailedList, setFileFailedList] = React.useState<UploadFile[]>([])

    // first memoe
    const statusMap: Record<string, UploadFile[]> = React.useMemo(() => {
        return _.groupBy([...fileSuccessList, ...fileFailedList], 'status') as any
    }, [fileSuccessList, fileFailedList])
    const fileMap: Record<string, UploadFile[]> = React.useMemo(() => {
        return _.keyBy(fileList, 'path') as any
    }, [fileList])
    const isMax = React.useMemo(() => fileList.length > UPLOAD_MAX, [fileList])
    const isExist = React.useCallback((file) => !!fileMap[getUploadName(file)], [fileMap])

    const handleReset = React.useCallback(() => {
        reset()
        setFileList([])
        setFileSuccessList([])
        setFileFailedList([])
    }, [])

    const beforeUpload = (file: UploadFile) => {
        // console.log('beforeUpload', file, !!isExist(file))
        if (isMax) {
            setFileFailedList((prev) => [
                ...prev,
                {
                    ...file,
                    status: 'error_max' as any,
                },
            ])
            return false
        }
        if (isExist(file)) {
            setFileFailedList((prev) => [
                ...prev,
                {
                    ...file,
                    status: 'error_exist' as any,
                },
            ])
            console.log('false')
            return false
        }
        return true
    }
    // use hooks
    const { uploadQueue } = useUploadingControl<UploadControlT>({
        onUpload: (file) => {
            console.log('onUpload', file)
            return fetch(file.oss, {
                method: 'PUT',
                body: file.file,
            })
        },
        onDone: (res) => {
            console.log('onDone', res)
            setFileSuccessList((prev) => [
                ...prev,
                {
                    ...pickAttr(res),
                    status: 'done',
                },
            ])
        },
    })
    const { getRootProps, getInputProps, isFocused, isDragAccept, isDragReject } = useDropzone({
        // Note how this callback is never invoked if drop occurs on the inner dropzone
        // @ts-ignore
        onDrop: async (files: UploadFile[]) => {
            const validateFiles: any[] = []
            files.forEach((file) => {
                if (!beforeUpload(file)) {
                    return false
                }
                validateFiles.push(file)
            })
            if (validateFiles.length == 0) return
            setFileList((prev) => [...prev, ...validateFiles])
            const signedUrls = await getSignUrls(validateFiles)
            const data = await sign(signedUrls, signPrefix)
            files.map((file) =>
                uploadQueue.next({
                    file,
                    oss: data.signedUrls[file.path],
                } as UploadControlT)
            )
        },
    })

    // memo
    const mostFrequentType = React.useMemo(() => {
        if (!fileList?.length) return null
        return findMostFrequentType(fileList)
    }, [rest.fileList])
    const total = fileList?.length ?? 0
    const totalSize = getReadableStorageQuantityStr(fileList?.reduce((acc, cur) => acc + cur.size ?? 0, 0) ?? 0)
    const getFile = (f: UploadFile) => (f.name ? f : fileMap?.[f.path])
    const getFileName = (f: UploadFile) => getUploadName(getFile(f))
    const style = React.useMemo(
        () => ({
            ...baseStyle,
            ...(isFocused ? focusedStyle : {}),
            ...(isDragAccept ? acceptStyle : {}),
            ...(isDragReject ? rejectStyle : {}),
        }),
        [isFocused, isDragAccept, isDragReject]
    )
    const dir = { directory: 'true', webkitdirectory: 'true' }
    const items = React.useMemo(() => fileList.map((file) => <ItemRender file={file} />), [fileList])
    // @ts-ignore
    const successCount = ['done', 'success'].reduce((acc, cur: any) => acc + (statusMap[cur]?.length ?? 0), 0)
    const errorCount = ['error', 'error_exist', 'error_max'].reduce(
        // @ts-ignore
        (acc, cur: any) => acc + (statusMap[cur]?.length ?? 0),
        0
    )
    console.log(statusMap)

    // effect
    useEffect(() => {
        const isDone = fileList?.every((item) => item.status !== 'uploading')
        const timer = setTimeout(() => {
            if (!isDone) {
                return
            }
            const type = fileList ? findMostFrequentType(fileList) : ''
            onChange?.({
                storagePath: signPrefix,
                type,
            })
        }, 1000)
        return () => {
            clearTimeout(timer)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [fileList, signPrefix])

    return (
        <div className={styles.drag}>
            {/* <input type='file' multiple {...dir} /> */}
            <div
                {...getRootProps({ className: 'dropzone ant-upload-btn' + ' ' })}
                style={{
                    ...style,
                    width: '100%',
                    flexShrink: 0,
                    flexBasis: 'auto',
                }}
            >
                <input {...getInputProps()} multiple {...dir} />
                <p>Drag 'n' drop some files here, or click to select files</p>
            </div>
            <div className='ant-upload-list'>{items}</div>
            {total > 0 && (
                <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-start', marginTop: 10 }}>
                    <div style={{ flex: 1 }}>
                        <p style={{ color: 'rgba(2,16,43,0.60)' }}>
                            {t('dataset.create.upload.total.desc', [successCount, totalSize])}
                        </p>
                        {errorCount ? (
                            <p style={{ color: '#CC3D3D', display: 'inline-flex', gap: '5px' }}>
                                {t('dataset.create.upload.error.desc')}
                                {[
                                    statusMap['error_exist']?.length && (
                                        <Text
                                            key='exist'
                                            tooltip={<pre>{statusMap['error_exist'].map(getFileName).join('\n')}</pre>}
                                        >
                                            {statusMap['error_exist'].length}&nbsp; (
                                            {t('dataset.create.upload.error.exist')})
                                        </Text>
                                    ),
                                    statusMap['error_max']?.length && (
                                        <Text
                                            key='max'
                                            tooltip={<pre>{statusMap['error_max'].map(getFileName).join('\n')}</pre>}
                                        >
                                            {t('dataset.create.upload.error.max')}&nbsp;
                                            {statusMap['error_max'].length}
                                        </Text>
                                    ),
                                ]}
                            </p>
                        ) : null}
                        {!mostFrequentType && (
                            <p style={{ color: '#CC3D3D' }}>{t('dataset.create.upload.error.type')}</p>
                        )}
                    </div>
                    <Button onClick={handleReset} as='link'>
                        {t('Reset')}
                    </Button>
                </div>
            )}
        </div>
    )
}

export { DraggerUpload }
export default DraggerUpload

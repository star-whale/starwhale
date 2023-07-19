import React, { useEffect } from 'react'
import { findMostFrequentType, getSignUrls, getUploadName, pickAttr } from './utils'
import Button from '../Button'
import { createUseStyles } from 'react-jss'
import useTranslation from '@/hooks/useTranslation'
import { getReadableStorageQuantityStr } from '../utils/index'
import Text from '../Text/Text'
import { useDropzone } from './react-dropzone'
import { deleteFiles, sign } from '@/domain/base/services/filestore'
import _ from 'lodash'
import useUploadingControl from './hooks/useUploadingControl'
import { UploadFile } from './types'
import { ItemRender } from './UploadItem'
import { useEvent } from '@starwhale/core'
import { useSign } from './hooks/useSign'
import axios from 'axios'
import IconFont from '../IconFont'

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
        '& .ant-upload-icon': {
            margin: '0 8px 0 12px',
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
            'height': '32px !important',
            'borderBottom': '1px solid #EEF1F6',
            '&:hover': {
                backgroundColor: '#EBF1FF !important',
            },
        },
        '& .ant-upload-list-item': {
            height: '31px !important',
            display: 'flex',
            alignItems: 'center',
            marginTop: '0px !important',
            fontSize: '14px !important',
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
    const { resetSign, signPrefix } = useSign()
    const [fileList, setFileList] = React.useState<UploadFile[]>([])
    const [fileSuccessList, setFileSuccessList] = React.useState<UploadFile[]>([])
    const [fileUploadingList, setFileUploadingList] = React.useState<UploadFile[]>([])
    const [fileFailedList, setFileFailedList] = React.useState<UploadFile[]>([])

    // first memoe
    const statusMap: Record<string, UploadFile[]> = React.useMemo(() => {
        return _.groupBy([...fileSuccessList, ...fileFailedList], 'status') as any
    }, [fileSuccessList, fileFailedList])
    const isMax = React.useMemo(() => fileList.length - fileFailedList.length > UPLOAD_MAX, [fileList, fileFailedList])
    const isExist = React.useCallback(
        (tmp) => fileSuccessList.some((f: any) => f.path === getUploadName(tmp)),
        [fileSuccessList]
    )
    const beforeUpload = (file: UploadFile) => {
        // console.log('beforeUpload', file, !!isExist(file))
        if (isMax) {
            setFileFailedList((prev) => [
                ...prev,
                {
                    ...file,
                    status: 'errorMax' as any,
                },
            ])
            return false
        }
        if (isExist(file)) {
            setFileFailedList((prev) => [
                ...prev,
                {
                    ...file,
                    status: 'errorExist' as any,
                },
            ])
            return false
        }
        return true
    }

    // use hooks
    const { uploadQueue, cancel: cancelQueue } = useUploadingControl<UploadControlT>({
        onUpload: useEvent((file) => {
            const uninterceptedAxiosInstance = axios.create()
            setFileUploadingList((prev) => {
                return [
                    ...prev,
                    {
                        ...pickAttr(file.file),
                        status: 'uploading',
                    },
                ]
            })
            return uninterceptedAxiosInstance({
                method: 'PUT',
                url: file.oss,
                headers: {
                    // https://github.com/star-whale/starwhale/blob/7c7ff4faabd947e430e924cfeeb8a0c17e57afb8/server/controller/src/main/java/ai/starwhale/mlops/domain/filestorage/FileStorageService.java#L93
                    'Content-Type': 'application/octet-stream',
                    'X-Requested-With': 'XMLHttpRequest',
                },
                data: file.file,
                onUploadProgress: (p) => {
                    setFileUploadingList((prev) => {
                        const index = prev.findIndex((f) => f.path === file.file.path)
                        if (index === -1) {
                            return [...prev]
                        }
                        const newFile = {
                            ...prev[index],
                            percent: 100 * (p.loaded / p.total),
                        }
                        prev.splice(index, 1, newFile)
                        return [...prev]
                    })
                    return 100 * (p.loaded / p.total)
                },
                onDownloadProgress: (p) => {
                    return 100 * (p.loaded / p.total)
                },
            })
        }),
        onDone: (file: UploadFile) => {
            setFileSuccessList((prev) => [
                ...prev,
                {
                    ...pickAttr(file),
                    status: 'done',
                },
            ])
            setFileUploadingList((prev) => prev.filter((f) => f.path !== file.path))
        },
        onError: (file: UploadFile) => {
            // console.log('onError', res)
            setFileFailedList((prev) => [
                ...prev,
                {
                    ...pickAttr(file),
                    status: 'errorUnknown',
                },
            ])
            setFileUploadingList((prev) => prev.filter((f) => f.path !== file.path))
        },
    })
    const { getRootProps, getInputProps, isFocused, isDragAccept, isDragReject } = useDropzone({
        // Note how this callback is never invoked if drop occurs on the inner dropzone
        // @ts-ignore
        onDrop: async (files: UploadFile[]) => {
            const validateFiles: any[] = []
            files.forEach((file) => {
                if (!beforeUpload(file)) {
                    return
                }
                validateFiles.push(file)
            })
            if (validateFiles.length === 0) return
            setFileList((prev) => [...prev, ...validateFiles])
            const signedUrls = await getSignUrls(validateFiles)
            const data = await sign(signedUrls, signPrefix)
            files.forEach((file) => {
                uploadQueue.next({
                    file,
                    oss: data.signedUrls[file.path],
                } as UploadControlT)
            })
        },
    })

    const handleReset = useEvent(() => {
        resetSign()
        setFileList([])
        setFileSuccessList([])
        setFileFailedList([])
        setFileUploadingList([])
        cancelQueue()
    })

    const handleRemove = useEvent(async (file: UploadFile) => {
        const name = getUploadName(file)
        await deleteFiles([name], signPrefix)
        setFileList((prev) => prev.filter((f) => f.path !== file.path))
        setFileSuccessList((prev) => prev.filter((f) => f.path !== file.path))
        setFileFailedList((prev) => prev.filter((f) => f.path !== file.path))
    })

    // memo
    const mostFrequentType = React.useMemo(() => {
        if (!fileSuccessList?.length) return null
        return findMostFrequentType(fileSuccessList)
    }, [fileSuccessList])
    const total = fileSuccessList?.length ?? 0
    const totalSize = getReadableStorageQuantityStr(fileSuccessList?.reduce((acc, cur) => acc + cur.size ?? 0, 0) ?? 0)
    const getFileName = (f: UploadFile) => getUploadName(f)
    const style = React.useMemo(
        () => ({
            ...baseStyle,
            ...(isFocused ? focusedStyle : {}),
            ...(isDragAccept ? acceptStyle : {}),
            ...(isDragReject ? rejectStyle : {}),
        }),
        [isFocused, isDragAccept, isDragReject]
    )
    // const dir = { directory: 'true', webkitdirectory: 'true' }
    const dir = {}
    const itemsUploading = React.useMemo(
        () =>
            fileUploadingList.map((file) => (
                <ItemRender key={file.path} file={file} percent={file.percent} onRemove={handleRemove} />
            )),
        [fileUploadingList, handleRemove]
    )
    const items = React.useMemo(
        () => fileSuccessList.map((file) => <ItemRender key={file.path} file={file} onRemove={handleRemove} />),
        [fileSuccessList, handleRemove]
    )
    // @ts-ignore
    const successCount = ['done', 'success'].reduce((acc, cur: any) => acc + (statusMap[cur]?.length ?? 0), 0)
    const errorCount = ['error', 'errorExist', 'errorMax', 'errorUnknown'].reduce(
        // @ts-ignore
        (acc, cur: any) => acc + (statusMap[cur]?.length ?? 0),
        0
    )

    // effect
    useEffect(() => {
        const timer = setTimeout(() => {
            const isDone = fileList?.every((item) => item.status !== 'uploading')

            if (!isDone) {
                return
            }
            const type = fileSuccessList ? findMostFrequentType(fileSuccessList) : ''
            onChange?.({
                storagePath: signPrefix,
                type,
            })
        }, 1000)
        return () => {
            clearTimeout(timer)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [fileList, fileSuccessList, signPrefix])

    // console.log(fileSuccessList, fileUploadingList)

    return (
        <div className={styles.drag}>
            {/* <input type='file' multiple {...dir} /> */}
            <div
                {...getRootProps({ className: 'dropzone ant-upload-btn' })}
                style={{
                    ...style,
                    width: '100%',
                    flexShrink: 0,
                    flexBasis: 'auto',
                }}
            >
                <input {...getInputProps()} multiple {...dir} />
                <p>{t('dataset.create.upload.drag.desc')}</p>
            </div>
            {(total > 0 || fileUploadingList.length > 0) && (
                <div style={{ display: 'flex', width: '100%', flex: 1, justifyContent: 'flex-end' }}>
                    <Button kind='secondary' onClick={handleReset} startEnhancer={<IconFont type='reset' />}>
                        {t('Reset')}
                    </Button>
                </div>
            )}
            <div className='ant-upload-list'>
                {itemsUploading}
                {items}
            </div>
            {total > 0 && (
                <div style={{ width: '100%', display: 'flex', gap: '10px', alignItems: 'flex-start', marginTop: 10 }}>
                    <div style={{ flex: 1 }}>
                        <p style={{ color: 'rgba(2,16,43,0.60)' }}>
                            {t('dataset.create.upload.total.desc', [successCount, totalSize])}
                        </p>
                        {errorCount ? (
                            <p style={{ color: '#CC3D3D', display: 'inline-flex', gap: '5px' }}>
                                {t('dataset.create.upload.error.desc')}
                                {statusMap.errorExist && statusMap.errorExist?.length && (
                                    <Text
                                        key='exist'
                                        tooltip={<pre>{statusMap.errorExist.map(getFileName).join('\n')}</pre>}
                                    >
                                        {statusMap.errorExist.length}&nbsp; ({t('dataset.create.upload.error.exist')})
                                    </Text>
                                )}
                                {statusMap.errorMax && statusMap.errorMax?.length && (
                                    <Text
                                        key='max'
                                        tooltip={<pre>{statusMap.errorMax.map(getFileName).join('\n')}</pre>}
                                    >
                                        {statusMap.errorMax.length}&nbsp;({t('dataset.create.upload.error.max')})
                                    </Text>
                                )}
                                {statusMap.errorUnknown && statusMap.errorUnknown?.length && (
                                    <Text
                                        key='max'
                                        tooltip={<pre>{statusMap.errorUnknown.map(getFileName).join('\n')}</pre>}
                                    >
                                        {statusMap.errorUnknown.length}&nbsp;({t('dataset.create.upload.error.unknown')}
                                        )
                                    </Text>
                                )}
                            </p>
                        ) : null}
                        {!mostFrequentType && (
                            <p style={{ color: '#CC3D3D' }}>{t('dataset.create.upload.error.type')}</p>
                        )}
                    </div>
                </div>
            )}
        </div>
    )
}

export { DraggerUpload }
export default DraggerUpload

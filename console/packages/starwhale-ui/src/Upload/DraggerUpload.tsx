import React, { useEffect } from 'react'
import Upload, { UploadFile } from 'antd/es/upload'
import { useUpload } from './hooks/useUpload'
import { findMostFrequentType, getUploadName } from './utils'
import Button from '../Button'
import IconFont from '../IconFont'
import { createUseStyles } from 'react-jss'
import useTranslation from '@/hooks/useTranslation'
import { getReadableStorageQuantityStr } from '../utils/index'
import Text from '../Text/Text'
const { Dragger } = Upload

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
            height: '170px !important',
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

type ValueT = { storagePath: string; type: string }
interface IDraggerUploadProps {
    value?: ValueT
    onChange?: (data: ValueT) => void
}

function DraggerUpload({ onChange }: IDraggerUploadProps) {
    const styles = useStyles()
    const [t] = useTranslation()
    const { getProps, statusMap, fileMap, signPrefix, reset } = useUpload()
    const rest = getProps()
    // @ts-ignore
    const successCount = ['done', 'success'].reduce((acc, cur: any) => acc + (statusMap[cur]?.length ?? 0), 0)
    const errorCount = ['error', 'error_exist', 'error_max'].reduce(
        // @ts-ignore
        (acc, cur: any) => acc + (statusMap[cur]?.length ?? 0),
        0
    )
    const fileList = rest.fileList

    const mostFrequentType = React.useMemo(() => {
        if (!fileList?.length) return null
        return findMostFrequentType(fileList)
    }, [rest.fileList])
    const [isDirectory, setIsDirectory] = React.useState(true)

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

    const total = fileList?.length ?? 0
    const totalSize = getReadableStorageQuantityStr(fileList?.reduce((acc, cur) => acc + cur.size ?? 0, 0) ?? 0)

    const getFile = (f: UploadFile) => (f.name ? f : fileMap?.[f.uid])
    const getFileName = (f: UploadFile) => getUploadName(getFile(f))

    return (
        <>
            <Dragger {...rest} directory={isDirectory} className={styles.drag}>
                <p className='ant-upload-drag-icon'>
                    <IconFont type='upload' size={24} />
                </p>
                <p className='ant-upload-text'>{t('dataset.create.upload.desc')}</p>
                {/* <p className='ant-upload-action'>
                    <Button
                        kind='secondary'
                        onClick={() => setIsDirectory(false)}
                        startEnhancer={<IconFont type='file2' />}
                    >
                        {t('Files')}
                    </Button>
                    <Button
                        kind='secondary'
                        onClick={() => setIsDirectory(true)}
                        startEnhancer={<IconFont type='file3' />}
                    >
                        {t('Directory')}
                    </Button>
                </p> */}
            </Dragger>
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
                    <Button onClick={reset} as='link'>
                        {t('Reset')}
                    </Button>
                </div>
            )}
        </>
    )
}

export { DraggerUpload }
export default DraggerUpload

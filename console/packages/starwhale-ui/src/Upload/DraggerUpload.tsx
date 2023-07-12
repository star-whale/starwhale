import React, { useEffect } from 'react'
import Upload from 'antd/es/upload'
import { useUpload } from './hooks/useUpload'
import { findMostFrequentType } from './utils'
const { Dragger } = Upload

type Value = { storagePath: string; type: string }
interface IDraggerUploadProps {
    value: Value
    onChange?: (data: Value) => void
}

function DraggerUpload({ onChange }: IDraggerUploadProps) {
    const { getProps, successNames, errorNames, signPrefix } = useUpload()
    const rest = getProps()
    const successCount = successNames.length
    const errorCount = errorNames.length

    useEffect(() => {
        const isDone = rest.fileList?.every((item) => item.status !== 'uploading')
        const timer = setTimeout(() => {
            if (!isDone) {
                return
            }
            onChange?.({
                storagePath: signPrefix,
                type: rest.fileList ? findMostFrequentType(rest.fileList) : '',
            })
        }, 1000)
        return () => {
            clearTimeout(timer)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [rest.fileList, signPrefix])

    return (
        <>
            <Dragger {...rest}>
                <p className='ant-upload-drag-icon'></p>
                <p className='ant-upload-text'>Click or drag file to this area to upload</p>
                <p className='ant-upload-hint'>
                    Support for a single or bulk upload. Strictly prohibited from uploading company data or other banned
                    files.
                </p>
            </Dragger>
            Success: {successCount}
            <br />
            Error: {errorCount}
        </>
    )
}

export { DraggerUpload }
export default DraggerUpload

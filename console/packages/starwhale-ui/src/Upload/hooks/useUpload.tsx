import React, { useState } from 'react'
import type { UploadFile, UploadProps, RcFile, UploadFileStatus } from 'antd/es/upload/interface'
import { deleteFiles, sign } from '@/domain/base/services/filestore'
import _ from 'lodash'
import { useSign } from './useSign'
import { useEvent } from '@starwhale/core'
import { getUploadName } from '../utils'
import { ItemRender } from '../UploadItem'

type StatusT = UploadFileStatus | 'error_exist' | 'error_max'

const UPLOAD_MAX = 1000

function useUpload(props: UploadProps = {}) {
    const { resetSign, signPrefix } = useSign()
    const [fileList, setFileList] = useState<UploadFile[]>([])
    const [fileMap, setFileMap] = useState<Record<string, UploadFile>>({})
    const [fileFailedList, setFileFailedList] = useState<UploadFile[]>([])

    const handleChange: UploadProps['onChange'] = async ({ file, fileList: fileListTmp }) => {
        const name = getUploadName(file)
        setFileMap((prev) => ({ ...prev, [name]: file, ...(_.keyBy(fileListTmp, 'uid') as any) }))
        setFileList([...fileListTmp.filter((f) => !fileFailedList.find((ff) => ff.uid === f.uid))])
    }

    const handleRemove = async (file: UploadFile) => {
        const name = getUploadName(file)
        await deleteFiles([name], signPrefix)
        setFileMap((prev) => ({ ...prev, [name]: undefined }))
        setFileList((prev) => prev.filter((f) => f.uid !== file.uid))
    }

    const itemRender = (
        originNode: React.ReactNode,
        file: UploadFile,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        currentFileList: UploadFile[]
    ): React.ReactNode => {
        // return originNode
        return <ItemRender file={file} />
    }

    // mock: 'https://www.mocky.io/v2/5cc8019d300000980a055e76',
    const action = async (file: RcFile) => {
        const name = getUploadName(file)
        const data = await sign([name], signPrefix)
        const oss = data.signedUrls[name]
        const status = await fetch(oss, {
            method: 'PUT',
            body: file,
        })
        return oss
    }

    const isMax = React.useMemo(() => fileList.length > UPLOAD_MAX, [fileList])
    const isExist = React.useCallback((file) => fileMap[getUploadName(file)], [fileMap])

    const reset = useEvent(() => {
        setFileList([])
        setFileMap({})
        setFileFailedList([])
        resetSign()
    })

    const beforeUpload: UploadProps['beforeUpload'] = async (file: UploadFile, fileListTmp: RcFile[]) => {
        // console.log(file)
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
            return false
        }
        return true
    }
    const statusMap: Record<StatusT, UploadFile[]> = React.useMemo(() => {
        return _.groupBy([...fileList, ...fileFailedList], 'status') as any
    }, [fileList, fileFailedList])

    // console.log(statusMap, fileMap)

    return {
        getProps: React.useCallback(
            (): UploadProps => ({
                ...props,
                name: 'file',
                multiple: true,
                directory: true,
                method: 'PUT',
                onChange: handleChange,
                fileList,
                itemRender,
                action,
                beforeUpload,
                progress: {
                    strokeColor: {
                        '0%': '#108ee9',
                        '100%': '#108ee9',
                    },
                    strokeWidth: 3,
                    showInfo: true,
                },
                // showUploadList: false,
            }),
            [props, fileList]
        ),
        signPrefix,
        fileMap,
        statusMap,
        reset,
        ItemRender,
    }
}

export { useUpload }

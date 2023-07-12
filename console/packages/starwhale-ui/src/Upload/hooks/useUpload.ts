import React, { useState } from 'react'
import type { UploadFile, UploadProps, RcFile } from 'antd/es/upload/interface'
import { sign } from '@/domain/base/services/filestore'
import _ from 'lodash'
import { useSign } from './useSign'
import { useEvent, useIfChanged } from '@starwhale/core'

function useUpload(props: UploadProps = {}) {
    const { getSign, resetSign, signPrefix } = useSign()
    const [fileList, setFileList] = useState<UploadFile[]>([])
    const [fileMap, setFileMap] = useState<Record<string, UploadFile>>({})

    const handleChange: UploadProps['onChange'] = async (info) => {
        const { uid } = info.file
        setFileMap((prev) => ({ ...prev, [uid]: info.file }))
        setFileList([...info.fileList])
    }

    const itemRender = (originNode: React.ReactNode, file: UploadFile, currentFileList: UploadFile[]) => {
        return originNode
    }

    // mock: 'https://www.mocky.io/v2/5cc8019d300000980a055e76',
    const action = async (file: RcFile) => {
        const prefix = await getSign()
        const data = await sign([file.webkitRelativePath ?? file.name], prefix)
        const oss = data.signedUrls[file.webkitRelativePath ?? file.name]
        return oss
    }

    const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
        // 1. type check
        return true
    }

    useIfChanged({
        fileList,
    })

    return {
        getProps: React.useCallback(
            (): UploadProps => ({
                ...props,
                name: 'file',
                multiple: true,
                directory: true,
                method: 'PUT',
                onChange: _.throttle(handleChange, 5000),
                fileList,
                itemRender,
                action,
                beforeUpload,
                // progress: {
                //     strokeColor: {
                //         '0%': '#108ee9',
                //         '100%': '#108ee9',
                //     },
                //     strokeWidth: 3,
                //     showInfo: true,
                // },
                // showUploadList: false,
            }),
            [props, fileList]
        ),
        signPrefix,
        fileMap,
        successNames: React.useMemo(() => {
            return fileList
                .filter((file) => file.status === 'done' || file.status === 'success')
                .map((file) => file.name)
        }, [fileList]),
        errorNames: React.useMemo(() => {
            return fileList.filter((file) => file.status === 'error').map((file) => file.name)
        }, [fileList]),
        reset: useEvent(() => {
            setFileList([])
            resetSign()
        }),
    }
}

export { useUpload }

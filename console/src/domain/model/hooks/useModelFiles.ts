import { useEffect, useState } from 'react'
import { fetchModelVersionFile, listModelVersionFiles } from '../services/modelVersion'
import { FileNode } from '@/domain/base/schemas/file'
import { getToken } from '@/api'

type FileNodeWithPathT = FileNode & {
    path: string[]
}

export function useModelFiles(projectId?: string, modelId?: string, modelVersionId?: string, root = '') {
    const [loadedFiles, setLoadedFiles] = useState<Record<string, any>>({})

    const loadFiles = async (path: string) => {
        if (!projectId || !modelId) return

        try {
            const data = await listModelVersionFiles(projectId, modelId, {
                version: modelVersionId,
                path,
            })
            const dataWithPath = data.files

            const walk = (filesTmp: FileNode[] = [], directory: string[] = []) => {
                filesTmp.forEach((file) => {
                    if ([...directory, file?.name].join('/') === path) {
                        // eslint-disable-next-line no-param-reassign
                        file.files = dataWithPath
                        return
                    }
                    if (file.files) walk(file.files, [...directory, file.name])
                })
            }
            if (path) walk(loadedFiles.files)
            else loadedFiles.files = dataWithPath

            setLoadedFiles({
                files: [...loadedFiles.files],
            })
        } catch (e) {
            // eslint-disable-next-line no-console
            console.log(e)
        }
    }

    const loadFileData = async (source: FileNodeWithPathT, version = '') => {
        try {
            return await fetchModelVersionFile(projectId, modelId, version, getToken(), source.path?.join('/'))
        } catch (e) {
            return ''
        }
    }

    useEffect(() => {
        if (projectId && modelId && modelVersionId) {
            loadFiles(root)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId, modelId, modelVersionId, root])

    return {
        loadedFiles,
        loadFiles,
        loadFileData,
    }
}

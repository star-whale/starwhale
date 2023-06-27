import { useEffect, useState } from 'react'
import { fetchModelVersionFile, listModelVersionFiles } from '../services/modelVersion'
import { FileNode } from '@/domain/base/schemas/file'
import { getToken } from '@/api'

type FileNodeWithPathT = FileNode & {
    path: string[]
}

export function useModelFiles(projectName?: string, modelName?: string, modelVersionId?: string, root = '') {
    const [loadedFiles, setLoadedFiles] = useState<Record<string, any>>({})

    const loadFiles = async (path: string) => {
        if (!projectName || !modelName) return

        try {
            const data = await listModelVersionFiles(projectName, modelName, {
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
            const data = await fetchModelVersionFile(
                projectName,
                modelName,
                version,
                getToken(),
                source.path?.join('/')
            )
            return data
        } catch (e) {
            return ''
        }
    }

    useEffect(() => {
        if (projectName && modelName && modelVersionId) {
            loadFiles(root)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectName, modelName, modelVersionId, root])

    return {
        loadedFiles,
        loadFiles,
        loadFileData,
    }
}

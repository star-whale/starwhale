import { fetchModelVersionFile } from '../services/modelVersion'
import { getToken } from '@/api'
import { useProject } from '@/domain/project/hooks/useProject'
import { useQuery } from 'react-query'
import { useModel } from './useModel'
import { useParams } from 'react-router-dom'

export function useCurrentModelFile(path = '') {
    const { project } = useProject()
    const { model } = useModel()
    const projectName = project?.name
    const modelName = model?.name
    const { modelVersionId } = useParams<{ modelVersionId: string }>()

    const info = useQuery(`fetchModelVersionFile:${projectName}:${modelName}:${modelVersionId}:${path}`, () =>
        fetchModelVersionFile(projectName, modelName, modelVersionId, getToken(), path)
    )

    return {
        content: info.data,
    }
}

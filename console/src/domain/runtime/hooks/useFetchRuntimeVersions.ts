import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listRuntimeVersions } from '../services/runtimeVersion'

export function useFetchRuntimeVersions(projectId: string, modelId: string, query: IListQuerySchema) {
    const modelVersionsInfo = useQuery(`fetchRuntimeVersions:${projectId}:${modelId}:${qs.stringify(query)}`, () =>
        listRuntimeVersions(projectId, modelId, query)
    )
    return modelVersionsInfo
}

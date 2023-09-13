import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listModels } from '../services/model'

export function useFetchModels(projectId: string, query: IListQuerySchema & { name?: string }) {
    const modelsInfo = useQuery(`fetchModels:${projectId}:${qs.stringify(query)}`, () => listModels(projectId, query))
    return modelsInfo
}

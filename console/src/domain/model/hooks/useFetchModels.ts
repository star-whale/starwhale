import { IListQuerySchema } from '@/schemas/list'
import { useQuery } from 'react-query'
import { listModels } from '../services/model'
import qs from 'qs'

export function useFetchModels(projectId: string, query: IListQuerySchema) {
    const modelsInfo = useQuery(`fetchModels:${qs.stringify(query)}`, () => listModels(projectId, query))
    return modelsInfo
}

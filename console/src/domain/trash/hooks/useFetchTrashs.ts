import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listTrashs } from '../services/trash'

export function useFetchTrashs(projectId: string, query: IListQuerySchema) {
    const trashsInfo = useQuery(`fetchTrashs:${qs.stringify(query)}`, () => listTrashs(projectId, query))
    return trashsInfo
}

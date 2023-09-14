import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listDatasets } from '../services/dataset'

export function useFetchDatasets(projectId: string, query: IListQuerySchema & { name?: string }) {
    const datasetsInfo = useQuery(`fetchDatasets:${projectId}:${qs.stringify(query)}`, () =>
        listDatasets(projectId, query)
    )
    return datasetsInfo
}

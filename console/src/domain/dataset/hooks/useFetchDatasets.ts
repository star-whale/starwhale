import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import { listDatasets } from '../services/dataset'
import qs from 'qs'

export function useFetchDatasets(projectId: string, query: IListQuerySchema) {
    const datasetsInfo = useQuery(`fetchDatasets:${projectId}:${qs.stringify(query)}`, () =>
        listDatasets(projectId, query)
    )
    return datasetsInfo
}

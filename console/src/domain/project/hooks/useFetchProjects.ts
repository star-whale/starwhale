import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import { listProjects } from '../services/project'
import qs from 'qs'

export function useFetchProjects(query: IListQuerySchema) {
    const projectsInfo = useQuery(`fetchProjects:${qs.stringify(query)}`, () => listProjects(query))
    return projectsInfo
}

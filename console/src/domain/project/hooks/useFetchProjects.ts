import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listProjects } from '../services/project'

export function useFetchProjects(query: IListQuerySchema & { sort?: string }) {
    const projectsInfo = useQuery(`fetchProjects:${qs.stringify(query)}`, () => listProjects(query))
    return projectsInfo
}

import { useQuery } from 'react-query'
import { fetchProject } from '../services/project'

export function useFetchProject(projectId?: string) {
    const projectInfo = useQuery(`fetchProject:${projectId}`, () => {
        // eslint-disable-next-line prefer-promise-reject-errors
        if (!projectId) return Promise.reject('fetchProject: no projectId stop fetching')

        return fetchProject(projectId)
    })
    return projectInfo
}

import { useQuery } from 'react-query'
import { fetchProject } from '../services/project'

export function useFetchProject(projectId?: string) {
    const projectInfo = useQuery(`fetchProject:${projectId}`, () => {
        if (!projectId) return Promise.reject(`fetchProject: no projectId stop fetching`)

        return fetchProject(projectId)
    })
    return projectInfo
}

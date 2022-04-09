import { useQuery } from 'react-query'
import { fetchProject } from '../services/project'

export function useFetchProject(projectId: string) {
    const projectInfo = useQuery(`fetchProject:${projectId}`, () => fetchProject(projectId))
    return projectInfo
}

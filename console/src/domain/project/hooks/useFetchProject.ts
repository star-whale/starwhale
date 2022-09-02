import { useEffect } from 'react'
import { useQuery } from 'react-query'
import { fetchProject } from '../services/project'

export function useFetchProject(projectId?: string) {
    const projectInfo = useQuery(`fetchProject:${projectId}`, () => fetchProject(projectId ?? ''), { enabled: false })

    useEffect(() => {
        if (projectId) {
            projectInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId])

    return projectInfo
}

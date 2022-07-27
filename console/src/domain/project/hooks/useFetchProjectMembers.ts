import { useQuery } from 'react-query'
import { listProjectRole } from '../services/project'

export function useFetchProjectMembers(projectId: string) {
    return useQuery(['fetchProjectMembers', projectId], () => {
        return listProjectRole(projectId)
    })
}

import { useQuery } from 'react-query'
import { listRoles } from '../services/project'

export function useFetchRoleList() {
    return useQuery('fetchRoleList', () => {
        return listRoles()
    })
}

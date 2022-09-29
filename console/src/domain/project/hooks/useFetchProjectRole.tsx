import React from 'react'
import { Role } from '@/api/const'
import { useQuery } from 'react-query'
import { useCurrentUser } from '../../../hooks/useCurrentUser'
import { listProjectRole } from '../services/project'

export function useFetchProjectRole(projectId: string) {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    const members = useQuery(
        ['fetchProjectMembers', projectId],
        () => {
            if (!projectId) return
            // eslint-disable-next-line consistent-return
            return listProjectRole(projectId)
        },
        { refetchOnWindowFocus: true, enabled: true }
    )

    React.useEffect(() => {
        if (projectId) {
            members.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId])

    const role = React.useMemo(() => {
        if (!members.data) return Role.NONE
        const member = members?.data?.find((m) => {
            return m?.user?.id === currentUser?.id
        })
        if (!member) return Role.NONE
        return (member?.role?.code as Role) ?? Role.NONE
    }, [currentUser, members])

    return {
        role,
    }
}

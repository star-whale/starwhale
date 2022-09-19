import React from 'react'
import { Role } from '@/api/WithAuth'
import { useCurrentUser } from '../../../hooks/useCurrentUser'
import { useFetchProjectMembers } from './useFetchProjectMembers'

export function useProjectRole(projectId: string) {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    const members = useFetchProjectMembers(projectId)

    const role = React.useMemo(() => {
        if (!members.data) return Role.NONE
        const member = members?.data?.find((m) => {
            return m?.user?.id === currentUser?.id
        })
        if (!member) return Role.NONE
        return member?.role?.code ?? Role.NONE
    }, [currentUser, members])

    return {
        role,
    }
}

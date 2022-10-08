import React from 'react'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { Role } from '@/api/const'

export function useUserRoles() {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    const systemRole = React.useMemo(() => {
        return currentUser?.systemRole ?? Role.NONE
    }, [currentUser])

    // const getProjectRole = React.useCallback(
    //     (id?: string) => {
    //         if (!id) return Role.NONE
    //         return currentUser?.projectRoles?.[id] ?? Role.NONE
    //     },
    //     [currentUser]
    // )
    // const projectRole = React.useMemo(() => {
    //     return getProjectRole(projectId)
    // }, [projectId, getProjectRole])

    return {
        systemRole,
        // projectRole,
        // getProjectRole,
    }
}

import React, { useMemo } from 'react'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary'
import { useProjectRole } from '@/domain/project/hooks/useProjectRole'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { IUserSchema } from '@user/schemas/user'
import { IPrivileges, Privileges, Role, RolePrivilege } from './const'
import { useSystemFeatures } from '@/domain/setting/hooks/useSystemFeatures'
import { ISystemFeaturesSchema } from '@/domain/setting/schemas/system'

const isRolePrivilege = (role: Role, id: string) => RolePrivilege[role]?.[id] ?? false
const isSystemDisablePrivilege = (systemFeatures: ISystemFeaturesSchema | undefined, id: keyof IPrivileges) =>
    systemFeatures?.disabled?.includes(id)
const isAdminPrivilege = (user: IUserSchema) => user.systemRole === Role.OWNER
const isWrongKey = (id: string) => !(id in Privileges)
const isCommunity = import.meta.env.VITE_EXTENDS !== 'true'

function Empty({ str }: any) {
    return <>{str}</>
}

export function useAuthPrivileged({ role, id }: { role: Role; id: keyof IPrivileges }) {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    const { systemFeatures } = useSystemFeatures()

    // priority: system global > admin > role
    const isPrivileged = useMemo(() => {
        if (!currentUser) return false
        if (isSystemDisablePrivilege(systemFeatures, id)) return false
        if (isAdminPrivilege(currentUser)) return true
        return isRolePrivilege(role, id)
    }, [currentUser, id, role, systemFeatures])

    return {
        isPrivileged,
    }
}

export default function WithAuth({
    role,
    id,
    children,
}: {
    role: Role
    id: keyof IPrivileges
    children: React.ReactNode
}) {
    const { isPrivileged } = useAuthPrivileged({ role, id })

    if (isWrongKey(id)) return <Empty str='wrong key' />
    if (typeof children === 'function') return <ErrorBoundary>{children(isPrivileged, isCommunity)}</ErrorBoundary>
    if (!isPrivileged) return <Empty />
    return <ErrorBoundary>{children ?? <Empty />}</ErrorBoundary>
}

export function WithCurrentAuth({ id, children }: { id: keyof IPrivileges; children: React.ReactNode }) {
    const { role } = useProjectRole()

    return (
        <WithAuth role={role} id={id}>
            {children}
        </WithAuth>
    )
}

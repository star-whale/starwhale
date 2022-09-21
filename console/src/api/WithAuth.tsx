import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary'
import { useProjectRole } from '@/domain/project/hooks/useProjectRole'
import React from 'react'
import { IPrivileges, Privileges, Role, RolePrivilege } from './const'

function hasPrivilege(role: Role, id: string) {
    return RolePrivilege[role]?.[id] ?? false
}

export default function WithAuth({
    role,
    id,
    children,
}: {
    role: Role
    id: keyof IPrivileges
    children: React.ReactElement | any
}) {
    if (!(id in Privileges)) return <>wrong key</>
    const isPrivileged = hasPrivilege(role, id)
    if (typeof children === 'function') {
        return children(isPrivileged)
    }
    if (!isPrivileged) return <></>
    return <ErrorBoundary>{children ?? <></>}</ErrorBoundary>
}

export function WithCurrentAuth({ id, children }: { id: keyof IPrivileges; children: React.ReactElement | any }) {
    const { role } = useProjectRole()
    return (
        <WithAuth role={role} id={id}>
            {children}
        </WithAuth>
    )
}

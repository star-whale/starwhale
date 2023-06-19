import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary'
import { useProjectRole } from '@/domain/project/hooks/useProjectRole'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import React from 'react'
import { IUserSchema } from '@user/schemas/user'
import { IPrivileges, Privileges, Role, RolePrivilege } from './const'
import { useSystemFeatures } from '@/domain/setting/hooks/useSystemFeatures'

const hasPrivilege = (role: Role, id: string) => {
    return RolePrivilege[role]?.[id] ?? false
}
const isAdmin = (user: IUserSchema) => user.systemRole === Role.OWNER
const isWrongKey = (id: string) => !(id in Privileges)

function Empty({ str }: any) {
    return <>{str}</>
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
    let isPrivileged = false
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    const { systemFeatures } = useSystemFeatures()

    if (!currentUser) return <Empty />
    if (isWrongKey(id)) return <Empty str='wrong key' />

    // priority: system global > admin > role
    if (systemFeatures.disabled?.includes(id)) isPrivileged = false
    else if (isAdmin(currentUser)) isPrivileged = true
    else isPrivileged = hasPrivilege(role, id)

    // render
    if (typeof children === 'function') {
        return children(isPrivileged)
    }
    if (!isPrivileged) return <Empty />
    return <ErrorBoundary>{children ?? <Empty />}</ErrorBoundary>
}

export function WithCurrentAuth({ id, children }: { id: keyof IPrivileges; children: React.ReactElement | any }) {
    const { role } = useProjectRole()

    return (
        <WithAuth role={role} id={id}>
            {children}
        </WithAuth>
    )
}

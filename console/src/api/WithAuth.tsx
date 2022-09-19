import React from 'react'

export const Privileges = {
    'member.delete': true,
    'member.update': true,
    'member.create': true,
    'member.read': true,
}
export type IPrivileges = typeof Privileges

export enum Role {
    OWNER = 'OWNER',
    GUEST = 'GUEST',
    MAINTAINER = 'MAINTAINER',
    NONE = 'NONE',
}

export const RolePrivilege: Record<Role, any> = {
    OWNER: {
        ...Privileges,
    },
    MAINTAINER: {
        ...Privileges,
    },
    GUEST: {
        ...Privileges,
        'member.delete': false,
        'member.update': false,
        'member.create': false,
    },
    NONE: {},
}

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
    return children
}

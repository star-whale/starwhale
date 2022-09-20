import React from 'react'

export const Privileges = {
    'member.delete': true,
    'member.update': true,
    'member.create': true,
    'member.read': true,
    'project.update': true,
    'project.delete': true,
    'evaluation.action': true,
    'evaluation.create': true,
    'runtime.version.revert': true,
    'model.version.revert': true,
    'dataset.version.revert': true,
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
        'member.update': false,
    },
    GUEST: {
        ...Privileges,
        'member.delete': false,
        'member.update': false,
        'member.create': false,
        'project.update': false,
        'project.delete': false,
        'evaluation.action': false,
        'evaluation.create': false,
        'runtime.version.revert': false,
        'model.version.revert': false,
        'dataset.version.revert': false,
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

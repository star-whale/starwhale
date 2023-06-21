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
    'model.version.serve': true,
    'dataset.version.revert': true,
    'evaluation.panel.save': true,
    'runtime.image.build': true,
    'task.execute': true,
    // global
    'online-eval': true,
    'job-pause': true,
    'job-resume': true,
    'job-dev': true,
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
        'member.delete': false,
        'member.create': false,
    },
    GUEST: {
        'member.read': true,
    },
    NONE: {},
}

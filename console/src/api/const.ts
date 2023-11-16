export const Privileges = {
    'member.delete': true,
    'member.update': true,
    'member.create': true,
    'member.read': true,
    'project.update': true,
    'project.delete': true,
    'evaluation.action': true,
    'evaluation.create': true,
    'report.create': true,
    'report.update': true,
    'report.delete': true,
    'runtime.version.revert': true,
    'model.run': true,
    'model.version.revert': true,
    'model.version.serve': true,
    'model.delete': true,
    'dataset.version.revert': true,
    'dataset.create': true,
    'dataset.create.read': true,
    'dataset.upload': true,
    'dataset.delete': true,
    'evaluation.panel.save': true,
    'runtime.image.build': true,
    'runtime.delete': true,
    'tag.edit': true,
    'task.execute': true,
    'job.pinOrUnpin': true,
    'job.cancel': true,
    'job.pause': true,
    'job.resume': true,
    'job.saveas': true,
    'ft.space.create': true,
    'ft.space.update': true,
    'ft.space.delete': true,
    'ft.run.create': true,
    // menu
    'project.menu.trash': true,
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

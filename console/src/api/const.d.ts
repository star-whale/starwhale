export declare const Privileges: {
    'member.delete': boolean;
    'member.update': boolean;
    'member.create': boolean;
    'member.read': boolean;
    'project.update': boolean;
    'project.delete': boolean;
    'evaluation.action': boolean;
    'evaluation.create': boolean;
    'runtime.version.revert': boolean;
    'model.version.revert': boolean;
    'dataset.version.revert': boolean;
};
export declare type IPrivileges = typeof Privileges;
export declare enum Role {
    OWNER = "OWNER",
    GUEST = "GUEST",
    MAINTAINER = "MAINTAINER",
    NONE = "NONE"
}
export declare const RolePrivilege: Record<Role, any>;

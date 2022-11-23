import { Role } from '@/api/const';
import { IResourceSchema } from '@/domain/base/schemas/resource';
export interface IUserRoleSchema {
    id: string;
    name: string;
    code: string;
    description: string;
}
export interface IUserSchema extends IResourceSchema {
    id: string;
    name: string;
    email: string;
    isEnabled: string;
    systemRole: Role;
    projectRoles: Record<string, Role>;
}
export interface IRegisterUserSchema {
    userName: string;
    userPwd: string;
}
export interface ILoginUserSchema {
    userName: string;
    userPwd: string;
    agreement: boolean;
}
export interface ISignupUserSchema extends ILoginUserSchema {
    agreement: boolean;
    callback: string;
}
export interface IUpdateUserSchema {
    userName: string;
    isEnabled: boolean;
}
export interface ICreateUserSchema {
    userName: string;
    userPwd: string;
}
export interface IChangePasswordSchema {
    originPwd: string;
    userPwd: string;
}
export interface INewUserSchema {
    userName: string;
    userPwd: string;
}
export interface ICloudLoginRespSchema {
    token?: string;
    verification?: string;
    step: string;
    title?: string;
}

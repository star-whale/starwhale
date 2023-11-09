import { Role } from '@/api/const'
import { IResourceSchema } from '@/domain/base/schemas/resource'

export interface IUserRoleSchema {
    id: string
    name: string
    code: string
    description: string
}

export interface IUserSchema extends IResourceSchema {
    id: string
    name: string
    email: string
    isEnabled: string
    systemRole: Role
    projectRoles: Record<string, Role>
}

export interface ILoginUserSchema {
    userName: string
    userPwd: string
    agreement: boolean
}

export interface ISignupUserSchema extends ILoginUserSchema {
    agreement: boolean
    callback: string
}

export interface IChangePasswordSchema {
    originPwd: string
    userPwd: string
}

export interface INewUserSchema {
    userName: string
    userPwd: string
}

export interface ICloudLoginRespSchema {
    token?: string
    verification?: string
    // step will be one of: strange_user, email_not_verified, account_not_created, account_created
    step: string
    title?: string
}

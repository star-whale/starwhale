import { IResourceSchema } from '@/domain/base/schemas/resource'

export interface IUserSchema extends IResourceSchema {
    name: string
    isEnabled: string
}

export interface IRegisterUserSchema {
    userName: string
    userPwd: string
}

export interface ILoginUserSchema {
    userName: string
    userPwd: string
}

export interface IUpdateUserSchema {
    userName: string
    isEnabled: boolean
}

export interface ICreateUserSchema {
    userName: string
    userPwd: string
}

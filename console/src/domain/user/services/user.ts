import axios from 'axios'
import {
    IUserSchema,
    IRegisterUserSchema,
    ILoginUserSchema,
    ICreateUserSchema,
    IChangePasswordSchema,
} from '../schemas/user'
import { IListQuerySchema, IListSchema } from '@/schemas/list'
import qs from 'qs'

export async function loginUser(data: ILoginUserSchema): Promise<IUserSchema> {
    const resp = await axios.post<IUserSchema>(`/api/v1/login`, data)
    console.log(resp)
    return resp.data
}

export async function fetchUser(userName: string): Promise<IUserSchema> {
    const resp = await axios.get<IUserSchema>(`/api/v1/users/${userName}`)
    return resp.data
}

export async function fetchCurrentUser(): Promise<IUserSchema> {
    const resp = await axios.get<IUserSchema>('/api/v1/user/current')
    return resp.data
}

export async function listUsers(query: IListQuerySchema): Promise<IListSchema<IUserSchema>> {
    const resp = await axios.get<IListSchema<IUserSchema>>('/api/v1/users', {
        params: query,
    })
    return resp.data
}

// export async function registerUser(data: IRegisterUserSchema): Promise<IUserSchema> {
//     const resp = await axios.post<IUserSchema>('/api/v1/auth/register', data)
//     return resp.data
// }

// export async function createUser(data: ICreateUserSchema): Promise<IUserSchema> {
//     const resp = await axios.post<IUserSchema>('/api/v1/users', data)
//     return resp.data
// }

export async function changePassword(data: IChangePasswordSchema): Promise<IUserSchema> {
    const resp = await axios.patch('/api/v1/auth/reset_password', data)
    return resp.data
}

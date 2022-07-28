import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import { IUserSchema, ILoginUserSchema, IChangePasswordSchema } from '../schemas/user'

export async function loginUser(data: ILoginUserSchema): Promise<IUserSchema> {
    const bodyFormData = new FormData()
    bodyFormData.append('userName', data.userName)
    bodyFormData.append('userPwd', data.userPwd)

    const resp = await axios({
        method: 'post',
        url: '/api/v1/login',
        data: bodyFormData,
        headers: { 'Content-Type': 'multipart/form-data' },
    })

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
    const resp = await axios.get<IListSchema<IUserSchema>>('/api/v1/user', {
        params: query,
    })
    return resp.data
}

export async function changePassword(data: IChangePasswordSchema) {
    // TODO change uri when backend ready
    const resp = await axios({
        method: 'put',
        url: '/api/v1/user/pwd',
        data: JSON.stringify(data),
        headers: { 'Content-Type': 'application/json' },
    })

    return resp.data
}

export async function changeUserState(userId: string, enable: boolean) {
    const resp = await axios({
        method: 'put',
        url: `/api/v1/user/${userId}/state`,
        data: JSON.stringify({ isEnabled: enable }),
        headers: { 'Content-Type': 'application/json' },
    })

    return resp.data
}

export async function createUser(userName: string, userPwd: string): Promise<IUserSchema> {
    const { data } = await axios({
        method: 'post',
        url: '/api/v1/user',
        data: JSON.stringify({ userName, userPwd }),
        headers: { 'Content-Type': 'application/json' },
    })
    return data
}

export async function checkUserPasswd(passwd: string) {
    const resp = await axios({
        method: 'post',
        url: '/api/v1/user/current/pwd',
        data: JSON.stringify({ currentUserPwd: passwd }),
        headers: { 'Content-Type': 'application/json' },
    })

    return resp.data
}

export async function changeUserPasswd(user: string, currentUserPwd: string, newPwd: string) {
    const resp = await axios({
        method: 'put',
        url: `/api/v1/user/${user}/pwd`,
        data: JSON.stringify({ currentUserPwd, newPwd }),
        headers: { 'Content-Type': 'application/json' },
    })

    return resp.data
}

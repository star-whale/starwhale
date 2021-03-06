import axios from 'axios'
import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import { IUserSchema, ILoginUserSchema } from '../schemas/user'

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
    const resp = await axios.get<IListSchema<IUserSchema>>('/api/v1/users', {
        params: query,
    })
    return resp.data
}

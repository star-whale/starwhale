import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import axios from 'axios'
import { IBaseImageSchema, ICreateBaseImageSchema, IDeviceSchema } from '../schemas/runtime'

export async function listBaseImages(query: IListQuerySchema): Promise<IListSchema<IBaseImageSchema>> {
    const resp = await axios.get('/api/v1/runtime/baseImage', {
        params: query,
    })
    return resp.data
}

export async function listDevices(query: IListQuerySchema): Promise<Array<IDeviceSchema>> {
    const resp = await axios.get('/api/v1/runtime/device', {
        params: query,
    })
    return resp.data
}

export async function createBaseImage(body: ICreateBaseImageSchema): Promise<any> {
    const resp = await axios.post('/api/v1/runtime/baseImage', body)
    return resp.data
}

export async function deleteBaseImage(imageId: string): Promise<any> {
    const resp = await axios.delete(`/api/v1/runtime/baseImage/${imageId}`)
    return resp.data
}

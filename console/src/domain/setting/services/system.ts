import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import axios from 'axios'
import {
    IAgentSchema,
    IDeviceSchema,
    ISystemFeaturesSchema,
    ISystemResourcePool,
    ISystemSettingSchema,
    ISystemVersionSchema,
} from '../schemas/system'

export async function fetchSystemVersion(): Promise<ISystemVersionSchema> {
    const resp = await axios.get('/api/v1/system/version')
    return resp.data
}

export async function listAgents(query: IListQuerySchema): Promise<IListSchema<IAgentSchema>> {
    const resp = await axios.get<IListSchema<IAgentSchema>>('/api/v1/system/agent', {
        params: query,
    })
    return resp.data
}

export async function deleteAgent(serialNumber: string): Promise<any> {
    const resp = await axios.delete(`/api/v1/system/agent?serialNumber=${serialNumber}`)
    return resp.data
}

export async function listDevices(query: IListQuerySchema): Promise<Array<IDeviceSchema>> {
    const resp = await axios.get('/api/v1/runtime/device', {
        params: query,
    })
    return resp.data
}

export async function fetchSystemSetting(): Promise<ISystemSettingSchema> {
    const resp = await axios.get('/api/v1/system/setting')
    return resp.data
}

export async function updateSystemSetting(data: string): Promise<any> {
    const resp = await axios.post('/api/v1/system/setting', data, {
        headers: {
            'Content-Type': 'text/plain',
        },
    })
    return resp.data
}

export async function fetchSystemResourcePool(): Promise<ISystemResourcePool[]> {
    const resp = await axios.get('/api/v1/system/resourcePool')
    return resp.data
}

export async function fetchSystemFeatures(): Promise<ISystemFeaturesSchema> {
    const resp = await axios.get('/api/v1/system/features')
    return resp.data
}

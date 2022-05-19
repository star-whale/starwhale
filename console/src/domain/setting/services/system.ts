import { IListQuerySchema, IListSchema } from '@/domain/base/schemas/list'
import axios from 'axios'
import { IAgentSchema, ISystemVersionSchema } from '../schemas/system'

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

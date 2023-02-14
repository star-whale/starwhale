import axios from 'axios'
import { IOnlineEvalStatusSchema } from '@project/schemas/OnlineEval'

export async function getOnlineEvalStatus(project: string, id: string): Promise<IOnlineEvalStatusSchema> {
    const { data } = await axios.get<IOnlineEvalStatusSchema>(`/api/v1/project/${project}/serving/${id}/status`)
    return data
}

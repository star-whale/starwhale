import axios from 'axios'

export async function getEvaluationViewConfig(projectId: string, name = 'evaluation'): Promise<any> {
    const resp = await axios.get<string>(`/api/v1/project/${projectId}/evaluation/view/config`, {
        params: {
            name,
        },
    })
    return resp.data
}

export async function setEvaluationViewConfig(projectId: string, data: any): Promise<any> {
    const resp = await axios.post<string>(`/api/v1/project/${projectId}/evaluation/view/config`, data)

    return resp.data
}

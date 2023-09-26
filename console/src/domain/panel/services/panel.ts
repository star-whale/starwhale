import axios from 'axios'

export async function fetchPanelSetting(projectId: string, key: string): Promise<any> {
    const resp = await axios.get(`/api/v1/panel/setting/${projectId}/${key}?slient=true`)
    return resp.data
}

export async function updatePanelSetting(projectId: string, key: string, data: Record<string, unknown>): Promise<any> {
    const resp = await axios.post(`/api/v1/panel/setting/${projectId}/${key}`, data)
    return resp.data
}

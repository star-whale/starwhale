import axios from 'axios'
import { IListSchema } from '@starwhale/core'
import { IReportSchema } from '@/domain/report/schemas/report'
import { IListQuerySchema } from '@base/schemas/list'
import { IReportVo } from '@/api'

export async function listReports(project: string, query: IListQuerySchema): Promise<IListSchema<IReportSchema>> {
    const { data } = await axios.get<IListSchema<IReportSchema>>(`/api/v1/project/${project}/report`, {
        params: query,
    })
    return data
}

export async function fetchReport(project: string, reportId: string): Promise<IReportVo> {
    const { data } = await axios.get<IReportVo>(`/api/v1/project/${project}/report/${reportId}`)
    return data
}

export async function fetchReportPreview(reportId: string): Promise<IReportVo> {
    const { data } = await axios.get<IReportVo>(`/api/v1/report/${reportId}/preview`)
    return data
}

export async function createReport(
    project: string,
    report: {
        title: string
        content?: string
        description?: string
    }
): Promise<string> {
    const { data } = await axios.post<string>(`/api/v1/project/${project}/report`, report)
    return data
}

export async function removeReport(projectId: string, reportId: number): Promise<string> {
    const { data } = await axios.delete<string>(`/api/v1/project/${projectId}/report/${reportId}`)
    return data
}

export async function updateReportShared(projectId: string, reportId: number, shared: boolean): Promise<string> {
    const { data } = await axios.put<string>(`/api/v1/project/${projectId}/report/${reportId}/shared`, null, {
        params: { shared },
    })
    return data
}

export async function updateReport(
    projectId: string,
    reportId: string,
    query: {
        title: string
        content?: string
        description?: string
    }
): Promise<string> {
    const { data } = await axios.put<string>(`/api/v1/project/${projectId}/report/${reportId}`, query)
    return data
}

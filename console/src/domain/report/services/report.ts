import axios from 'axios'
import { IListSchema } from '@starwhale/core'
import { IReportDetailSchema, IReportSchema } from '@/domain/report/schemas/report'
import { IListQuerySchema } from '@base/schemas/list'

export async function listReports(project: string, query: IListQuerySchema): Promise<IListSchema<IReportSchema>> {
    const { data } = await axios.get<IListSchema<IReportSchema>>(`/api/v1/project/${project}/report`, {
        params: query,
    })
    return data
}

export async function fetchReport(project: string, reportId: string): Promise<IReportDetailSchema> {
    const { data } = await axios.get<IReportDetailSchema>(`/api/v1/project/${project}/report/${reportId}`)
    return data
}

export async function createReport(project: string, report: IReportDetailSchema): Promise<IReportDetailSchema> {
    const { data } = await axios.post<IReportDetailSchema>(`/api/v1/project/${project}/report`, report)
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

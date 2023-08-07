import axios from 'axios'
import { IListSchema } from '@starwhale/core'
import { IReportDetailSchema, IReportSchema } from '@/domain/report/schemas/report'

const fakeId = (): number => Math.floor(Math.random() * 1000)

const fakeReport = (): IReportDetailSchema => ({
    id: fakeId(),
    title: 'loooooooooooooooooooooooooooooooooooooooooooooong title',
    description: 'loooooooooooooooooooooooooooooooooooooooooooooong description',
    content: 'fake content',
    createdTime: '2020-01-01 00:00:00',
    updatedTime: '2020-01-01 00:00:01',
    owner: 'fake owner',
    shared: fakeId() % 2,
})

const dev = process.env.NODE_ENV === 'development'

export async function listReports(project: string, titleFilter?: string): Promise<IListSchema<IReportSchema>> {
    if (dev) {
        return {
            list: [fakeReport(), fakeReport(), fakeReport()].filter(
                (i) => titleFilter === undefined || i.title.includes(titleFilter)
            ),
            total: 1,
        }
    }
    const resp = await axios.get<IListSchema<IReportSchema>>(`/api/v1/project/${project}/report`, {
        params: { titleFilter },
    })
    return resp.data
}

export async function fetchReport(project: string, reportId: string): Promise<IReportDetailSchema> {
    if (dev) {
        return fakeReport()
    }
    const resp = await axios.get<IReportDetailSchema>(`/api/v1/project/${project}/report/${reportId}`)
    return resp.data
}

export async function createReport(data: IReportDetailSchema): Promise<IReportDetailSchema> {
    const resp = await axios.post<IReportDetailSchema>('/api/v1/report', data)
    return resp.data
}

export async function removeReport(projectId: string, reportId: string): Promise<string> {
    const { data } = await axios.delete<string>(`/api/v1/project/${projectId}/report/${reportId}`)
    return data
}

export async function updateReportShared(projectId: string, reportId: number, shared: boolean): Promise<string> {
    if (dev) {
        return 'success'
    }
    const { data } = await axios.put<string>(`/api/v1/project/${projectId}/report/${reportId}/shared`, { shared })
    return data
}

import qs from 'qs'

const VERSION_PREFIX_CNT = 2

export function tableDataLink(
    projectId: string,
    datasetId: string,
    datasetVersion: string,
    query: {
        uri: string
        authName: string
        offset: string
        size: string
        Authorization?: string
    }
) {
    return `/api/v1/project/${projectId}/dataset/${datasetId}/version/${datasetVersion}/link?${qs.stringify(query)}`
}

export function tableNameOfDataset(projectId: string, datasetId: string, datasetVersion: string) {
    return `project/${projectId}/dataset/${datasetId}/${datasetVersion.substring(
        0,
        VERSION_PREFIX_CNT
    )}/${datasetVersion}/meta`
}

export function tableNameOfResult(projectId: string, evaluationId: string) {
    return `project/${projectId}/eval/${evaluationId}/summary`
}

export function tableNameOfSummary(projectId: string) {
    return `project/${projectId}/eval/summary`
}

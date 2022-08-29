import qs from 'qs'

const VERSION_PREFIX_CNT = 2

export function tableDataLink(
    projectId: string,
    datasetName: string,
    datasetVersionName: string,
    query: {
        uri: string
        authName: string
        offset: string
        size: string
        Authorization?: string
    }
) {
    return `/api/v1/project/${projectId}/dataset/${datasetName}/version/${datasetVersionName}/link?${qs.stringify(
        query
    )}`
}

export function tableNameOfDataset(projectName: string, datasetName: string, datasetVersionName: string) {
    return `project/${projectName}/dataset/${datasetName}/${datasetVersionName.substring(
        0,
        VERSION_PREFIX_CNT
    )}/${datasetVersionName}/meta`
}

export function tableNameOfResult(projectName: string, evaluationUuid: string) {
    return `project/${projectName}/eval/${evaluationUuid}/results`
}

export function tableNameOfSummary(projectName: string) {
    return `project/${projectName}/eval/summary`
}

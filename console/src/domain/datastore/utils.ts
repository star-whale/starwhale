import qs from 'qs'

const VERSION_PREFIX_CNT = 2

export function tableDataLink(
    projectId: string,
    datasetName: string,
    datasetVersionName: string,
    query: {
        uri: string
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
    return `project/${projectName}/eval/${evaluationUuid.substring(0, VERSION_PREFIX_CNT)}/${evaluationUuid}/results`
}

export function tableNameOfConfusionMatrix(projectName: string, evaluationUuid: string) {
    return `project/${projectName}/eval/${evaluationUuid.substring(
        0,
        VERSION_PREFIX_CNT
    )}/${evaluationUuid}/confusion_matrix/binarylabel`
}

export function tableNameOfRocAuc(projectName: string, evaluationUuid: string, label: string) {
    return `project/${projectName}/eval/${evaluationUuid.substring(
        0,
        VERSION_PREFIX_CNT
    )}/${evaluationUuid}/roc_auc/${label}`
}

export function tableNameOfSummary(projectName: string) {
    return `project/${projectName}/eval/summary`
}

export function tablesOfEvaluation(projectName: string, evaluationUuid: string) {
    return `project/${projectName}/eval/${evaluationUuid.substring(0, VERSION_PREFIX_CNT)}/${evaluationUuid}`
}

export function showTableName(name: string) {
    if (name.includes('/summary')) return name.split('/').slice(3).join('/')
    return name.split('/').slice(5).join('/')
}

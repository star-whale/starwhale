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
    return `/api/v1/project/${projectId}/dataset/${datasetName}/blob?${qs.stringify(query)}`
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

export function tablesOfEvaluation(projectName: string, evaluationUuid = '') {
    return `project/${projectName}/eval/${evaluationUuid.substring(0, VERSION_PREFIX_CNT)}/${evaluationUuid}`
}

export function showTableName(name: string) {
    if (name.includes('/summary')) return name.split('/').slice(3).join('/')
    return name.split('/').slice(5).join('/')
}

export function unhexlify(str: string) {
    const f = new Uint8Array(8)
    let j = 0
    for (let i = 0, l = str.length; i < l; i += 2) {
        f[j] = parseInt(str.substr(i, 2), 16)
        j++
    }
    return f
}

export function hexlifyString(str: string) {
    let result = ''
    const padding = '00'
    for (let i = 0, l = str.length; i < l; i++) {
        const digit = str.charCodeAt(i).toString(16)
        const padded = (padding + digit).slice(-2)
        result += padded
    }
    return result
}

export function hexlify(str: Uint8Array) {
    let result = ''
    const padding = '00'
    for (let i = 0, l = str.length; i < l; i++) {
        const digit = str[i].toString(16)
        const padded = (padding + digit).slice(-2)
        result += padded
    }
    return result
}

export const isBasicType = (v: string) =>
    ['BOOL', 'INT8', 'INT16', 'INT32', 'INT64', 'FLOAT16', 'FLOAT32', 'FLOAT64', 'STRING'].includes(v)

export const isSearchColumns = (v: string) => !v.startsWith('_')

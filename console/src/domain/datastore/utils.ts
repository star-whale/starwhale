import qs from 'qs'
// @ts-ignore
import struct from '@aksel/structjs'

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
    return `project/${projectName}/eval/${evaluationUuid.substring(0, VERSION_PREFIX_CNT)}/${evaluationUuid}/results`
}

export function tableNameOfConfusionMatrix(projectName: string, evaluationUuid: string) {
    return `project/${projectName}/eval/${evaluationUuid.substring(
        0,
        VERSION_PREFIX_CNT
    )}/${evaluationUuid}/confusion_matrix/binarylabel`
}

export function tableNameOfRocAuc(projectName: string, evaluationUuid: string) {
    return `project/${projectName}/eval/${evaluationUuid.substring(0, VERSION_PREFIX_CNT)}/${evaluationUuid}/roc_auc/0`
}

export function tableNameOfSummary(projectName: string) {
    return `project/${projectName}/eval/summary`
}

export const unhexlify = function (str: string) {
    const f = new Uint8Array(8)
    let j = 0
    for (let i = 0, l = str.length; i < l; i += 2) {
        f[j] = parseInt(str.substr(i, 2), 16)
        j++
    }
    const s = struct('>d')
    return s.unpack(f.buffer)[0]
}

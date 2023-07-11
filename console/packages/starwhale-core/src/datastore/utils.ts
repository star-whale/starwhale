import qs from 'qs'
import { DataTypes } from './constants'

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
    return `/api/v1/project/${projectId}/dataset/${datasetName}/uri?${qs.stringify(query)}`
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

export const isStringType = (v: string) => v === DataTypes.STRING
export const isBoolType = (v: string) => v === DataTypes.BOOL
export const isNumbericType = (v: string) =>
    [
        DataTypes.FLOAT16,
        DataTypes.FLOAT32,
        DataTypes.FLOAT64,
        DataTypes.INT16,
        DataTypes.INT32,
        DataTypes.INT64,
        DataTypes.INT8,
    ].includes(v as DataTypes)

export const isComplexType = (v: string) =>
    [DataTypes.LIST, DataTypes.TUPLE, DataTypes.MAP, DataTypes.OBJECT, DataTypes.BYTES].includes(v as DataTypes)
export const isBasicType = (v: string) => isNumbericType(v) || isStringType(v) || isBoolType(v)
export const isSearchColumns = (v: string) => {
    if (v.startsWith('_')) return false
    if (v.startsWith('sys/_')) return false
    if (v.split('@')[1]?.startsWith('_')) return false

    return true
}

export class LRUCache<Key, Value> {
    private maxSize: number

    private cache: Map<Key, Value>

    private accessedKeys: Set<Key>

    constructor(maxSize: number) {
        this.maxSize = maxSize
        this.cache = new Map<Key, Value>()
        this.accessedKeys = new Set<Key>()
    }

    has(key: Key): boolean {
        return this.cache.has(key)
    }

    get(key: Key): Value | undefined {
        if (this.cache.has(key)) {
            // Move the key to the front of the accessedKeys set
            this.accessedKeys.delete(key)
            this.accessedKeys.add(key)
            return this.cache.get(key)
        }
        return undefined
    }

    put(key: Key, value: Value) {
        if (this.cache.has(key)) {
            // Move the key to the front of the accessedKeys set
            this.accessedKeys.delete(key)
            this.accessedKeys.add(key)
            this.cache.set(key, value)
        } else {
            if (this.cache.size >= this.maxSize) {
                // Remove the least recently used key from the cache and accessedKeys set
                const lruKey = this.accessedKeys.values().next().value
                this.accessedKeys.delete(lruKey)
                this.cache.delete(lruKey)
            }
            // Add the new key to the front of the accessedKeys set
            this.accessedKeys.add(key)
            this.cache.set(key, value)
        }
    }
}

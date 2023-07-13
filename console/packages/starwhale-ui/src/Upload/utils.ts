import type { RcFile, UploadFile } from 'antd/es/upload/interface'
import { fi } from 'date-fns/locale'

export const getUploadType = (file: UploadFile) => {
    if (file.type?.startsWith('image')) return 'IMAGE'
    if (file.type?.startsWith('video')) return 'VIDEO'
    if (file.type?.startsWith('audio')) return 'AUDIO'
    if (file.name?.includes('.csv')) return 'CSV'
    if (file.name?.includes('.json')) return 'JSON'
    if (file.name?.includes('.jsonL')) return 'JSONL'
    return ''
}

export const getUploadName = (file: UploadFile | RcFile | any) => {
    if (!file) return ''
    if (file.originFileObj) return file.originFileObj?.webkitRelativePath ?? file.name
    return file.webkitRelativePath ?? file.name
}

function findMostFrequentType(arr: UploadFile[]) {
    const typeCounts: Record<string, number> = {}

    for (let i = 0; i < arr.length; i++) {
        const type = getUploadType(arr[i])
        if (typeCounts[type]) {
            typeCounts[type]++
        } else {
            typeCounts[type] = 1
        }
    }

    let mostFrequentType = ''
    let maxCount = 0

    // eslint-disable-next-line no-restricted-syntax
    for (const type in typeCounts) {
        if (typeCounts[type] > maxCount) {
            mostFrequentType = type
            maxCount = typeCounts[type]
        }
    }

    return mostFrequentType
}

export const getSignName = (file: UploadFile) => file.originFileObj?.webkitRelativePath ?? file.name

export const getSignUrls = (fileList: UploadFile[]) =>
    fileList.reduce((acc, file) => {
        if (file.originFileObj?.webkitRelativePath) {
            acc.push(file.originFileObj?.webkitRelativePath)
        }
        return acc
    }, [] as string[])

export { findMostFrequentType }

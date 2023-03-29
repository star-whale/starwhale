import Alias from '@/components/Alias'
import Shared from '@/components/Shared'
import { formatTimestampDateTime } from '@/utils/datetime'
import React from 'react'
import { IDatasetVersionSchema } from '../schemas/datasetVersion'

export function DatasetLabel({
    version,
    dataset,
    isProjectShow = false,
    style = {},
}: {
    version: IDatasetVersionSchema
    dataset: IDatasetSchema
    isProjectShow?: boolean
}) {
    const share = <Shared shared={version.shared} isTextShow={false} />
    const alias = <Alias alias={version.alias} />
    const p = dataset ? [dataset.ownerName, dataset.projectName, dataset.datasetName].join('/') : ''
    const v = [version.versionName ? version.versionName.substring(0, 8) : ''].join(':')
    const title = [p, v, version.createdTime ? formatTimestampDateTime(version.createdTime) : ''].join('/')

    return (
        <div
            style={{
                display: 'inline-flex',
                gap: '4px',
                justifyContent: 'flex-start',
                alignItems: 'center',
                ...style,
            }}
            title={title}
        >
            {share} {isProjectShow ? [p, v].join('/') : v} {alias}
        </div>
    )
}

export default DatasetLabel

import Alias from '@/components/Alias'
import Shared from '@/components/Shared'
import { formatTimestampDateTime } from '@/utils/datetime'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import React from 'react'
import { IDatasetTreeSchema } from '../schemas/dataset'
import { IDatasetTreeVersionSchema } from '../schemas/datasetVersion'
import { getAliasStr } from '@base/utils/alias'

export const DatasetLabelContainer = themedStyled('div', () => ({
    display: 'inline-flex',
    gap: '4px',
    justifyContent: 'flex-start',
    alignItems: 'center',
    height: '100%',
    width: '100%',
    minWidth: '0',
}))

export const DatasetLabelText = themedStyled('div', () => ({
    minWidth: '0',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    fontFamily: 'Roboto Mono',
}))

export function getDatastLabel(version: IDatasetTreeVersionSchema, dataset?: IDatasetTreeSchema) {
    const p = dataset ? [dataset.ownerName, dataset.projectName, dataset.datasetName].join('/') : ''
    const name = version?.versionName ?? version?.name
    const v = (name ?? '').substring(0, 8)
    const title = [p, v, version.alias, version.createdTime ? formatTimestampDateTime(version.createdTime) : '']
        .filter((tmp) => !!tmp)
        .join('/')

    return title
}

export function DatasetLabel({
    version,
    dataset,
    isProjectShow = false,
    style = {},
}: {
    version: IDatasetTreeVersionSchema
    dataset?: IDatasetTreeSchema
    isProjectShow?: boolean
    style?: React.CSSProperties
}) {
    const share = <Shared shared={version.shared} isTextShow={false} />
    const alias = <Alias alias={getAliasStr(version)} />
    const p = dataset ? [dataset.ownerName, dataset.projectName, dataset.datasetName].join('/') : ''
    const name = version?.versionName ?? version?.name
    const v = (name ?? '').substring(0, 8)
    const title = [p, v, version.alias, version.createdTime ? formatTimestampDateTime(version.createdTime) : '']
        .filter((tmp) => !!tmp)
        .join('/')

    return (
        <DatasetLabelContainer style={style} title={title}>
            {share} <DatasetLabelText>{isProjectShow ? [p, v].join('/') : v}</DatasetLabelText> {alias}
        </DatasetLabelContainer>
    )
}

export default DatasetLabel

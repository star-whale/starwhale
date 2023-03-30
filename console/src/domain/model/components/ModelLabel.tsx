import Alias from '@/components/Alias'
import { formatTimestampDateTime } from '@/utils/datetime'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import React from 'react'
import { IModelVersionSchema } from '../schemas/modelVersion'

export const ModelLabelContainer = themedStyled('div', () => ({
    display: 'inline-flex',
    gap: '4px',
    justifyContent: 'flex-start',
    alignItems: 'center',
    height: '100%',
    width: '100%',
}))

export const ModelLabelText = themedStyled('div', () => ({
    display: 'inline-flex',
    minWidth: '0',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
}))

export function ModelLabel({
    version,
    isProjectShow = false,
    style = {},
}: {
    version: IModelVersionSchema & { versionName?: string }
    isProjectShow?: boolean
    style?: React.CSSProperties
}) {
    // const share = <Shared shared={version.shared} isTextShow={false} />
    const share = ''
    const alias = <Alias alias={version.alias} />
    const p = ''
    const name = version?.versionName ?? version?.name
    const v = (name ?? '').substring(0, 8)
    const title = [p, v, version.createdTime ? formatTimestampDateTime(version.createdTime) : '']
        .filter((tmp) => !!tmp)
        .join('/')

    return (
        <ModelLabelContainer style={style} title={title}>
            {share} <ModelLabelText>{isProjectShow ? [p, v].join('/') : v}</ModelLabelText> {alias}
        </ModelLabelContainer>
    )
}

export default ModelLabel

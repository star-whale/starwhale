import Alias from '@/components/Alias'
import Shared from '@/components/Shared'
import { formatTimestampDateTime } from '@/utils/datetime'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import React from 'react'
import { IRuntimeTreeSchema } from '../schemas/runtime'
import { IRuntimeTreeVersionSchema } from '../schemas/runtimeVersion'

export const RuntimeLabelContainer = themedStyled('div', () => ({
    display: 'inline-flex',
    gap: '4px',
    justifyContent: 'flex-start',
    alignItems: 'center',
    height: '100%',
    width: '100%',
}))

export const RuntimeLabelText = themedStyled('div', () => ({
    display: 'inline-flex',
    minWidth: '0',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    fontFamily: 'Roboto Mono',
}))

export function RuntimeLabel({
    version,
    runtime,
    isProjectShow = false,
    style = {},
}: {
    version: IRuntimeTreeVersionSchema
    runtime?: IRuntimeTreeSchema
    isProjectShow?: boolean
    style?: React.CSSProperties
}) {
    const share = <Shared shared={version.shared} isTextShow={false} />
    const alias = <Alias alias={version.alias} />
    const p = runtime ? [runtime.ownerName, runtime.projectName, runtime.runtimeName].join('/') : ''
    const name = version?.versionName ?? version?.name
    const v = (name ?? '').substring(0, 8)
    const title = [p, v, version.createdTime ? formatTimestampDateTime(version.createdTime) : '']
        .filter((tmp) => !!tmp)
        .join('/')

    return (
        <RuntimeLabelContainer style={style} title={title}>
            {share} <RuntimeLabelText>{isProjectShow ? [p, v].join('/') : v}</RuntimeLabelText> {alias}
        </RuntimeLabelContainer>
    )
}

export default RuntimeLabel

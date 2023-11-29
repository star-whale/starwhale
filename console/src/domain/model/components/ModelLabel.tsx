import Alias from '@/components/Alias'
import { formatTimestampDateTime } from '@/utils/datetime'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import React from 'react'
import Shared from '@/components/Shared'
import { getAliasStr } from '@base/utils/alias'
import { IModelVersionViewVo, IModelVersionVo, IModelViewVo } from '@/api'
import { IHasTagSchema } from '@base/schemas/resource'
import useTranslation from '@/hooks/useTranslation'
import StatusTag from '@/components/Tag/StatusTag'

export const ModelLabelContainer = themedStyled('div', () => ({
    display: 'inline-flex',
    gap: '4px',
    justifyContent: 'flex-start',
    alignItems: 'center',
    height: '100%',
    width: '100%',
    minWidth: 0,
    overflow: 'hidden',
}))

export const ModelLabelText = themedStyled('div', () => ({
    display: 'inline-flex',
    minWidth: '0',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    fontFamily: 'Roboto Mono',
    height: 'inherit',
    flexShrink: 0,
}))

export function getModelLabel(version: IModelVersionViewVo, model?: IModelViewVo) {
    const p = model ? [model.ownerName, model.projectName, model.modelName].join('/') : ''
    const name = version.versionName
    const v = (name ?? '').substring(0, 8)
    const title = [p, v, version?.alias, version.createdTime ? formatTimestampDateTime(version.createdTime) : '']
        .filter((tmp) => !!tmp)
        .join('/')

    return title
}

export function ModelLabel({
    version,
    model,
    isProjectShow = false,
    style = {},
    hasDraft = false,
}: {
    version: IModelVersionVo | IModelVersionViewVo
    model?: IModelViewVo
    isProjectShow?: boolean
    style?: React.CSSProperties
    hasDraft?: boolean
}) {
    const [t] = useTranslation()
    const share = <Shared shared={!!version.shared} isTextShow={false} />
    const alias = <Alias alias={getAliasStr(version as IHasTagSchema)} />
    const draft = (
        <div className='flex gap-2px lh-none flex-shrink-0'>
            {version.draft === true && <StatusTag>{t('ft.job.model.release.mode.draft')}</StatusTag>}
            {version.draft === false && (
                <StatusTag kind='positive'>{t('ft.job.model.release.mode.released')}</StatusTag>
            )}
        </div>
    )
    const p = model ? [model.ownerName, model.projectName, model.modelName].join('/') : ''
    const name = (version as IModelVersionViewVo)?.versionName ?? (version as IModelVersionVo)?.name
    const v = (name ?? '').substring(0, 8)
    const title = [p, v, version.createdTime ? formatTimestampDateTime(version.createdTime) : '']
        .filter((tmp) => !!tmp)
        .join('/')

    return (
        <ModelLabelContainer style={style} title={title}>
            {hasDraft && draft} {share} <ModelLabelText>{isProjectShow ? [p, v].join('/') : v}</ModelLabelText> {alias}
        </ModelLabelContainer>
    )
}

export default ModelLabel

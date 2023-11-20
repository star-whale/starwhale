import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import JobStatus from '@/domain/job/components/JobStatus'
import Alias from '@/components/Alias'
import { TextLink } from '@/components/Link'
import { api } from '@/api'
import { useParams } from 'react-router-dom'
import useFineTuneColumns, { OVERVIEW_COLUMNS_KEYS } from '@/domain/space/hooks/useFineTuneColumns'

export default function FineTuneOverview() {
    const [t] = useTranslation()

    const { projectId, spaceId, fineTuneId } = useParams<{ projectId: any; spaceId: any; fineTuneId; any }>()
    const info = api.useFineTuneInfo(projectId, spaceId, fineTuneId)
    const { job } = info.data || {}

    const datasetUris = job?.datasetList?.map((v) => {
        const uri = `project/${1}/dataset/${v?.name}/version/${v?.version?.name}`
        const to = `/projects/${projectId}/datasets/${v?.id}/versions/${v?.version?.id}/overview`
        return (
            <TextLink key={uri} to={to} baseStyle={{ maxWidth: 'none' }}>
                {uri}
            </TextLink>
        )
    })

    const modelUri = `project/${1}/model/${job?.modelName}/version/${job?.modelVersion}`
    const modelTo = `/projects/${projectId}/models/${job?.model?.id}/versions/${job?.model?.version?.id}/overview`
    const modelLink = (
        <TextLink key={modelUri} to={modelTo} baseStyle={{ maxWidth: 'none' }}>
            {modelUri}
        </TextLink>
    )

    const runtimeUri = `project/${1}/runtime/${job?.runtime?.name}/version/${job?.runtime?.version?.name}`
    const runtimeTo = `/projects/${projectId}/runtimes/${job?.runtime?.id}/versions/${job?.runtime?.version?.id}/overview`
    const runtimeLink = (
        <TextLink key={runtimeUri} to={runtimeTo} baseStyle={{ maxWidth: 'none' }}>
            {runtimeUri}
        </TextLink>
    )

    const { renderCell, columns } = useFineTuneColumns({
        keys: OVERVIEW_COLUMNS_KEYS,
    })
    const renderer = renderCell(info.data)

    return (
        <div className='flex-column overflow-auto'>
            {columns.map((v) => (
                <div
                    key={v?.key}
                    style={{
                        display: 'flex',
                        gap: '20px',
                        borderBottom: '1px solid #EEF1F6',
                        lineHeight: '44px',
                        flexWrap: 'nowrap',
                        fontSize: '14px',
                        paddingLeft: '12px',
                    }}
                >
                    <div className='basis-170px overflow-hidden text-ellipsis flex-shrink-0 color-[rgba(2,16,43,0.60)]'>
                        {v?.title}
                    </div>
                    <div className='py-13px lh-18px'>{renderer(v.key)}</div>
                </div>
            ))}
        </div>
    )
}

import React from 'react'
import { useParams } from 'react-router-dom'
import FineTuneRunsListCard from './FineTuneRunsListCard'
import { api } from '@/api'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'
import { useCreation } from 'ahooks'
import FineTuneJobActionGroup from '@/domain/space/components/FineTuneJobActionGroup'
import { useQueryArgs } from '@/hooks/useQueryArgs'
import RouteOverview from './RouteOverview'

export default function FineTuneListCard() {
    const { projectId, spaceId } = useParams<{ projectId: any; spaceId: any }>()
    const { query, updateQuery } = useQueryArgs()
    const { renderCell } = useFineTuneColumns()
    const info = api.useListFineTune(projectId, spaceId)

    const { fineTuneId } = query
    const isExpand = !!fineTuneId
    const url = isExpand && `/projects/${projectId}/spaces/${spaceId}/fine-tunes/${fineTuneId}/overview`

    const fineTune = React.useMemo(
        () => info.data?.list?.find((v) => String(v.id) === fineTuneId),
        [fineTuneId, info.data?.list]
    )

    const title = useCreation(() => {
        if (!fineTune) return null
        const renderer = renderCell(fineTune)
        return (
            <>
                <div className='flex items-center font-600'>{renderer('baseModelName')}</div>
                <div className='flex-1 items-center mt-6px mb-auto'>{renderer('baseModelVersionAlias')}</div>
            </>
        )
    }, [fineTuneId, fineTune])

    const params = {
        projectId,
        spaceId,
        fineTuneId: fineTune?.id,
        jobId: fineTune?.job?.id,
        job: fineTune?.job,
    }

    return (
        <div className={`grid gap-15px content-full ${isExpand ? 'grid-cols-[360px_1fr]' : 'grid-cols-1'}`}>
            <FineTuneRunsListCard
                data={info.data}
                isExpand={isExpand}
                onView={(id) => updateQuery({ fineTuneId: id })}
                onRefresh={() => info.refetch()}
                viewId={fineTuneId}
                params={params}
            />
            {isExpand && (
                <RouteOverview
                    title={title}
                    url={url}
                    onClose={() => updateQuery({ fineTuneId: undefined })}
                    extraActions={<FineTuneJobActionGroup onRefresh={() => info.refetch()} {...params} />}
                />
            )}
        </div>
    )
}

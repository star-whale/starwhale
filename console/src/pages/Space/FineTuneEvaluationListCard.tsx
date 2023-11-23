import React from 'react'
import EvaluationListCard from './Evaluation/EvaluationListCard'
import { api } from '@/api'
import RouteOverview from './RouteOverview'
import useFineTuneEvaluation from '@/domain/space/hooks/useFineTuneEvaluation'
import EvalJobActionGroup from '@/domain/space/components/EvalJobActionGroup'

export default function FineTuneEvaluationListCard() {
    const config = useFineTuneEvaluation()
    const { projectId, spaceId, gotoList, jobId, routes } = config
    const info = api.useGetJob(projectId, jobId)
    const [key, forceUpdate] = React.useReducer((s) => s + 1, 0)
    const onRefresh = () => {
        info.refetch()
        forceUpdate()
    }
    const isExpand = !!jobId
    const url = isExpand && routes.evaluationOverview

    // const title = useCreation(() => {
    //     if (!fineTune) return null
    //     const renderer = renderCell(fineTune)
    //     return (
    //         <>
    //             <div className='flex items-center font-600'>{renderer('baseModelName')}</div>
    //             <div className='flex-1 items-center mt-6px mb-auto'>{renderer('baseModelVersionAlias')}</div>
    //         </>
    //     )
    // }, [fineTuneId, fineTune])

    const params = {
        projectId,
        jobId,
        spaceId,
        job: info.data,
    }
    const actionBar = <EvalJobActionGroup onRefresh={onRefresh} {...params} />

    return (
        <div className={`grid gap-15px content-full ${isExpand ? 'grid-cols-[360px_1fr]' : 'grid-cols-1'}`}>
            {!isExpand && (
                <div className='ft-list content-full'>
                    <EvaluationListCard {...config} />
                </div>
            )}
            {isExpand && (
                <RouteOverview key={key} url={url} onClose={gotoList} extraActions={actionBar} hasFullscreen={false} />
            )}
        </div>
    )
}

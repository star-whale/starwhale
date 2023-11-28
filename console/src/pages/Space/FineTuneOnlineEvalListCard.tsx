import React from 'react'
import { api } from '@/api'
import RouteOverview from './RouteOverview'
import { useCreation } from 'ahooks'
import { Button, GridResizer } from '@starwhale/ui'
import useFineTuneOnlineEval from '@/domain/space/hooks/useOnlineEval'
import FineTuneOnlineEvalJobCard from './FineTuneOnlineEvalJobCard'
import useTranslation from '@/hooks/useTranslation'
import { useHistory } from 'react-router-dom'

const GRID_LAYOUT = [
    // RIGHT:
    '0px 40px 1fr',
    // MIDDLE:
    '350px 40px 1fr',
]

export default function FineTuneOnlineEvalListCard() {
    const [t] = useTranslation()
    const config = useFineTuneOnlineEval()
    const history = useHistory()
    const { projectId, spaceId, gotoList, routes } = config
    const [key, forceUpdate] = React.useReducer((s) => s + 1, 0)
    const info = api.useListOnlineEval(projectId, spaceId)
    const onRefresh = () => {
        info.refetch()
        forceUpdate()
    }

    const title = useCreation(() => {
        return null
    }, [info])

    // const params = {
    //     projectId,
    //     spaceId,
    //     job: info.data,
    // }
    // const actionBar = <EvalJobActionGroup onRefresh={onRefresh} {...params} />

    console.log(routes)

    return (
        <div className='content-full pt-12px'>
            <GridResizer
                left={() => (
                    <div className='flex flex-col w-full gap-10px'>
                        <Button
                            isFull
                            size='compact'
                            onClick={() => {
                                history.push(`/projects/${projectId}/new_fine_tune_online/${spaceId}`)
                            }}
                        >
                            {t('create')}
                        </Button>
                        <div className='content-full'>
                            <FineTuneOnlineEvalJobCard />{' '}
                        </div>
                    </div>
                )}
                gridLayout={GRID_LAYOUT}
                right={() => (
                    <RouteOverview
                        key={key}
                        url={routes.onlineServings}
                        onClose={gotoList}
                        extraActions={null}
                        hasFullscreen={false}
                        title={title}
                    />
                )}
                draggable={false}
                resizebar='expand'
            />
        </div>
    )
}

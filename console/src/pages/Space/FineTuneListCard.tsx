import React from 'react'
import { usePage } from '@/hooks/usePage'
import useTranslation from '@/hooks/useTranslation'
import { useHistory, useParams, useRouteMatch } from 'react-router-dom'
import { useRouteContext } from '@/contexts/RouteContext'
import FineTuneRunsListCard from './FineTuneRunsListCard'
import { ExtendButton } from '@starwhale/ui'
import { api } from '@/api'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'
import { useBoolean, useCreation, useToggle } from 'ahooks'
import { headerHeight } from '@/consts'

const RouteBar = ({ onClose, onFullScreen, fullscreen }) => {
    const history = useHistory()
    const match = useRouteMatch('/projects/:projectId/spaces/:spaceId/fine-tunes/:fineTuneId?/overview')

    console.log(match)

    console.log(history, history.length)

    // @ts-ignore
    const isLastOne = history.index === 0

    return (
        <div className='ft-route-bar absolute right-20px top-20px z-1 gap-16px flex'>
            <ExtendButton icon='fullscreen' styleas={['iconnormal', 'nopadding']} onClick={onFullScreen} />
            {!fullscreen && (
                <ExtendButton
                    icon='close'
                    styleas={['iconnormal', 'nopadding']}
                    onClick={() => (isLastOne ? onClose?.() : history.go(-1))}
                />
            )}
        </div>
    )
}

const RouteOverview = ({ url, onClose, title }) => {
    const { RoutesInline } = useRouteContext()
    const [fullscreen, { toggle }] = useBoolean(false)

    if (!RoutesInline) return null

    return (
        <div
            className='ft-route border-1 p-20px content-full relative bg-white rounded-sm'
            style={
                fullscreen
                    ? {
                          position: 'fixed',
                          top: headerHeight,
                          left: 0,
                          right: 0,
                          bottom: 0,
                          zIndex: 100,
                      }
                    : undefined
            }
        >
            <div className='h-56px w-full'>{title}</div>
            <div className='content-full'>
                <RoutesInline initialEntries={url && [url]} key={url}>
                    <RouteBar onClose={onClose} fullscreen={fullscreen} onFullScreen={toggle} />
                </RoutesInline>
            </div>
        </div>
    )
}

export default function FineTuneListCard() {
    const [page] = usePage()
    const [t] = useTranslation()
    const { projectId, spaceId } = useParams<{ projectId: any; spaceId: any }>()
    const [expandFineTuneId, setExpandFineTuneId] = React.useState<number | undefined>(undefined)
    //
    const { renderCell } = useFineTuneColumns()
    const info = api.useListFineTune(projectId, spaceId, {
        ...page,
    })
    //
    const isExpand = !!expandFineTuneId
    const url = isExpand && `/projects/${projectId}/spaces/${spaceId}/fine-tunes/${expandFineTuneId}/overview`

    const title = useCreation(() => {
        const fineTune = info.data?.list?.find((v) => v.id === expandFineTuneId)
        const renderer = renderCell(fineTune)
        if (!fineTune) return null
        return (
            <>
                <div className='flex items-center font-600'>{renderer('baseModelName')}</div>
                <div className='flex-1 items-center mt-12px mb-auto'>{renderer('baseModelVersionAlias')}</div>
            </>
        )
    }, [expandFineTuneId])

    return (
        <div className={`grid gap-20px content-full ${isExpand ? 'grid-cols-[360px_1fr]' : 'grid-cols-1'}`}>
            <FineTuneRunsListCard
                data={info.data}
                isExpand={isExpand}
                onView={setExpandFineTuneId}
                viewId={expandFineTuneId}
            />
            {isExpand && <RouteOverview title={title} url={url} onClose={() => setExpandFineTuneId(undefined)} />}
        </div>
    )
}

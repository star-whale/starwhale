import React, { useEffect, useLayoutEffect } from 'react'
import { usePage } from '@/hooks/usePage'
import useTranslation from '@/hooks/useTranslation'
import { useHistory, useParams } from 'react-router-dom'
import { useRouteContext } from '@/contexts/RouteContext'
import FineTuneRunsListCard from './FineTuneRunsListCard'
import { ExtendButton } from '@starwhale/ui'
import { api } from '@/api'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'
import { useBoolean, useCreation } from 'ahooks'
import { headerHeight } from '@/consts'
import { useEventCallback } from '@starwhale/core'

const FINT_TUNE_ROUTE = '/projects/(.*)?/spaces/(.*)?/fine-tunes/(.*)?'

const RouteBar = ({ onClose, onFullScreen, fullscreen, onLocationChange }) => {
    const [isRoot, setIsRoot] = React.useState(true)
    const history = useHistory()

    useEffect(() => {
        const unsubscribe = history?.listen((location) => {
            const isFineTune = Boolean(location.pathname.match(FINT_TUNE_ROUTE))
            onLocationChange?.({
                history,
                location,
            })
            setIsRoot(isFineTune)
        })
        return () => {
            unsubscribe()
        }
    }, [history, onLocationChange])

    return (
        <div className='ft-route-bar absolute left-20px right-20px top-20px z-1 gap-16px flex justify-between'>
            <div className=''>
                {!isRoot && (
                    <ExtendButton
                        icon='arrow_left'
                        styleas={['iconnormal', 'nopadding']}
                        onClick={() => history.go(-1)}
                    />
                )}
            </div>
            <div className='flex flex-shrink-0 gap-16px'>
                <ExtendButton icon='fullscreen' styleas={['iconnormal', 'nopadding']} onClick={onFullScreen} />
                {!fullscreen && (
                    <ExtendButton
                        icon='close'
                        styleas={['iconnormal', 'nopadding']}
                        onClick={() => {
                            if (isRoot) return onClose?.()
                            // @ts-ignore
                            if (history.entries?.[0]) return history.replace(history.entries[0])
                            return history.go(-1)
                        }}
                    />
                )}
            </div>
        </div>
    )
}

const RouteOverview = ({ url, onClose, title }) => {
    const { RoutesInline } = useRouteContext()
    const [fullscreen, { toggle }] = useBoolean(false)
    const [isRouteAtFineTune, setIsRouteAtFineTune] = React.useState(true)
    const ref = React.useRef<HTMLDivElement>(null)
    const [rect, setRect] = React.useState<{ left: number; top: number } | undefined>(undefined)

    const handelInlineLocationChange = useEventCallback(({ history, location, match }) => {
        setIsRouteAtFineTune(Boolean(location.pathname.match(FINT_TUNE_ROUTE)))
    })

    useEffect(() => {
        setIsRouteAtFineTune(Boolean(url?.match(FINT_TUNE_ROUTE)))
    }, [url])

    useLayoutEffect(() => {
        if (!ref.current) return
        const table = ref.current.getBoundingClientRect()
        setRect({
            left: table?.left,
            top: table?.top,
        })
    }, [ref])

    if (!RoutesInline) return null

    return (
        <div className='ft-route relative content-full' ref={ref}>
            <div
                className={`ft-route flex-col relative border-1 bg-white rounded-sm ${
                    !isRouteAtFineTune && 'card-shadow-md overflow-visible mg-4px'
                }`}
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
                        : {
                              position: 'fixed',
                              top: rect?.top,
                              left: rect?.left,
                              right: '20px',
                              bottom: 0,
                              zIndex: 100,
                          }
                }
            >
                <div className={`h-56px w-full p-20px ${!isRouteAtFineTune && 'border-b'}`}>
                    {isRouteAtFineTune && title}
                </div>
                <div className='content-full p-20px '>
                    <RoutesInline initialEntries={url && [url]} key={url}>
                        <RouteBar
                            onClose={onClose}
                            fullscreen={fullscreen}
                            onFullScreen={toggle}
                            onLocationChange={handelInlineLocationChange}
                        />
                    </RoutesInline>
                </div>
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
                <div className='flex-1 items-center mt-6px mb-auto'>{renderer('baseModelVersionAlias')}</div>
            </>
        )
    }, [expandFineTuneId])

    return (
        <div className={`grid gap-15px content-full ${isExpand ? 'grid-cols-[360px_1fr]' : 'grid-cols-1'}`}>
            <FineTuneRunsListCard
                data={info.data}
                isExpand={isExpand}
                onView={setExpandFineTuneId}
                onRefresh={() => info.refetch()}
                viewId={expandFineTuneId}
            />
            {isExpand && <RouteOverview title={title} url={url} onClose={() => setExpandFineTuneId(undefined)} />}
        </div>
    )
}

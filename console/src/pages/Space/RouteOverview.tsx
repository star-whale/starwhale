import React, { useEffect, useLayoutEffect } from 'react'
import { useHistory, useRouteMatch } from 'react-router-dom'
import { useRouteContext } from '@/contexts/RouteContext'
import { ExtendButton } from '@starwhale/ui'
import { useBoolean } from 'ahooks'
import { headerHeight } from '@/consts'
import { useTrace } from '@starwhale/core'

const RouteBar = ({ onClose, onFullScreen, fullscreen, rootUrl, onRootChange, extraActions }) => {
    const trace = useTrace('route-inline')
    const [isRoot, setIsRoot] = React.useState(true)
    const history = useHistory()
    const match = useRouteMatch('/projects/:projectId/spaces/:spaceId/:path?/:fineTuneId?/:path2?')
    const { path, path2, fineTuneId, url } = match?.params ?? ({} as any)

    useEffect(() => {
        // in case of redirect to current page
        if (!fineTuneId && !path2 && path === 'fine-tune-runs') {
            history.replace(rootUrl)
        }
        onRootChange?.(fineTuneId)
        setIsRoot(fineTuneId)
        // eslint-disable-next-line
    }, [history, path, path2, fineTuneId, url])

    trace(history.location)
    console.log('rootUrl', rootUrl, history)

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
                {isRoot && extraActions}
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

const RouteOverview = ({ url, onClose, title, extraActions, hasFullscreen = true }) => {
    const { RoutesInline } = useRouteContext()
    const [fullscreen, { toggle }] = useBoolean(false)
    const [isRoot, setIsRoot] = React.useState(true)
    const ref = React.useRef<HTMLDivElement>(null)
    const [rect, setRect] = React.useState<{ left: number; top: number } | undefined>(undefined)

    useEffect(() => {
        setIsRoot(true)
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

    const style: React.CSSProperties = fullscreen
        ? {
              position: 'fixed',
              top: headerHeight,
              left: 0,
              right: 0,
              bottom: 0,
              zIndex: 11,
          }
        : {
              position: 'fixed',
              top: rect?.top,
              left: rect?.left,
              right: '20px',
              bottom: 0,
              zIndex: 11,
          }

    return (
        <div className='ft-route relative content-full' ref={ref}>
            <div
                className={`ft-route flex-col relative border-1 bg-white rounded-sm ${
                    !isRoot && 'card-shadow-md overflow-visible mg-4px'
                }`}
                style={style}
            >
                <div className={`h-56px w-full px-20px ${!isRoot && 'border-b'}`}>{isRoot && title}</div>
                <div className='content-full p-20px'>
                    <RoutesInline initialEntries={url && [url]} key={url}>
                        <RouteBar
                            rootUrl={url}
                            onRootChange={setIsRoot}
                            onClose={onClose}
                            fullscreen={fullscreen}
                            onFullScreen={toggle}
                            extraActions={extraActions}
                        />
                    </RoutesInline>
                </div>
            </div>
        </div>
    )
}

export default RouteOverview

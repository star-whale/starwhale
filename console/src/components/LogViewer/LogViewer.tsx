import '@patternfly/react-core/dist/styles/base.css'

import React, { useLayoutEffect, useRef } from 'react'
import { LogViewer, LogViewerSearch } from '@patternfly/react-log-viewer'
import {
    Button,
    Tooltip,
    Toolbar,
    ToolbarContent,
    ToolbarGroup,
    ToolbarItem,
    ToolbarToggleGroup,
} from '@patternfly/react-core'
import OutlinedPlayCircleIcon from '@patternfly/react-icons/dist/esm/icons/outlined-play-circle-icon'
import ExpandIcon from '@patternfly/react-icons/dist/esm/icons/expand-icon'
import PauseIcon from '@patternfly/react-icons/dist/esm/icons/pause-icon'
import PlayIcon from '@patternfly/react-icons/dist/esm/icons/play-icon'
import EllipsisVIcon from '@patternfly/react-icons/dist/esm/icons/ellipsis-v-icon'
import DownloadIcon from '@patternfly/react-icons/dist/esm/icons/download-icon'
import HistoryIcon from '@patternfly/react-icons/dist/esm/icons/history-icon'
import useWebSocket from '@/hooks/useWebSocket'
import SWSelect from '@starwhale/ui/Select'
import './LogView.scss'
import useTranslation from '@/hooks/useTranslation'
import { useFullscreen, useInterval, useToggle } from 'react-use'
import { useEvent } from '@starwhale/core'

function useSourceData({ ws = '', data }: { ws?: string; data?: string }) {
    const [content, setContent] = React.useState<any[]>([])
    const messageRef = React.useRef<any[]>([])
    const [, startTransition] = React.useTransition()

    // offline data
    React.useEffect(() => {
        if (data) {
            const tmp = data.split('\n')
            setContent(tmp)
            messageRef.current = tmp
        }
    }, [data])

    // online ws changed
    React.useEffect(() => {
        if (ws) {
            messageRef.current = []
            setContent([])
        }
    }, [ws])

    useInterval(() => {
        if (content.length === messageRef.current.length) return
        startTransition(() => {
            setContent([...messageRef.current])
        })
    }, 200)

    // useInterval(() => {
    //     messageRef.current.push(Date.now())
    // }, 10)

    useWebSocket({
        wsUrl: ws,
        onMessage: React.useCallback((d) => {
            messageRef.current.push(d)
        }, []),
        // disable close truncate content, because it's async
        onClose: React.useCallback(() => {}, []),
    })

    return {
        content,
    }
}

const ComplexToolbarLogViewer = ({
    sources: dataSources,
}: {
    sources: {
        type: string
        id: string
        data?: string
        ws?: string
    }[]
}) => {
    const [isPaused, setIsPaused] = React.useState(false)
    const [currentItemCount, setCurrentItemCount] = React.useState(0)
    const [renderData, setRenderData] = React.useState('')
    const [selectedDataSource, setSelectedDataSource] = React.useState(dataSources[0]?.id)
    const [buffer, setBuffer] = React.useState([])
    const [linesBehind, setLinesBehind] = React.useState(0)
    const logViewerRef = React.useRef<any>()
    const [t] = useTranslation()
    const [, startTransition] = React.useTransition()

    const selectedDataSourceObj = dataSources?.find((d) => d.id === selectedDataSource) || {}
    const { content: selectedData } = useSourceData(selectedDataSourceObj)

    const reset = React.useCallback(() => {
        setIsPaused(false)
        setLinesBehind(0)
        setBuffer([])
        setCurrentItemCount(0)
        if (logViewerRef && logViewerRef.current) {
            logViewerRef.current?.scrollToBottom()
        }
    }, [logViewerRef])

    React.useEffect(() => {
        // console.log('current  length = ', currentItemCount, selectedData.length)
        if (currentItemCount !== selectedData.length) {
            startTransition(() => {
                setBuffer(selectedData.slice(currentItemCount) as any)
            })
        }
    }, [selectedData, currentItemCount])

    React.useEffect(() => {
        startTransition(() => {
            // console.log('buffer length = ', buffer.length)
            if (buffer.length === 0) {
                setLinesBehind(0)
                return
            }

            if (isPaused) {
                setLinesBehind(buffer.length)
            } else {
                setCurrentItemCount(selectedData.length)
                setRenderData(selectedData.join('\n'))
                setBuffer([])
                if (logViewerRef && logViewerRef.current) {
                    logViewerRef.current?.scrollToBottom()
                }
            }
        })
    }, [isPaused, buffer, currentItemCount, selectedData])

    const ref = useRef<Element | null>(null)
    const [show, toggle] = useToggle(false)

    const onDownloadClick = useEvent(() => {
        const element = document.createElement('a')
        const dataToDownload = [selectedData.join('\n')]
        const file = new Blob(dataToDownload, { type: 'text/plain' })
        element.href = URL.createObjectURL(file)
        element.download = `${selectedDataSource}.txt`
        document.body.appendChild(element)
        element.click()
        document.body.removeChild(element)
    })

    const scrollToBottom = useEvent(() => {
        if (logViewerRef && logViewerRef.current) {
            logViewerRef.current?.scrollToBottom()
        }
    })

    const onScrollTop = useEvent(() => {
        if (logViewerRef && logViewerRef.current) {
            logViewerRef.current?.scrollToItem(0)
        }
    })

    const onExpand = useEvent(() => {
        toggle()
        setTimeout(() => {
            scrollToBottom()
        }, 1000)
    })

    // @ts-ignore
    const onScroll = useEvent(({ scrollOffsetToBottom, scrollUpdateWasRequested }) => {
        if (!scrollUpdateWasRequested) {
            // console.log('onscroll', scrollUpdateWasRequested, scrollOffsetToBottom, scrollOffset)
            if (scrollOffsetToBottom > 3) {
                setIsPaused(true)
            }
        }
    })

    useFullscreen(ref, show, {
        onClose: () => {
            toggle(false)
            setTimeout(() => {
                scrollToBottom()
            }, 1000)
        },
    })

    useLayoutEffect(() => {
        if (!ref.current) ref.current = document.querySelector('#complex-toolbar-demo')
    }, [])

    React.useEffect(() => {
        reset()
        setSelectedDataSource(dataSources[0]?.id)
        toggle(true)
        setTimeout(() => {
            scrollToBottom()
        }, 1000)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [dataSources])

    const ControlButton = React.useMemo(
        () => (
            <Button
                variant={isPaused ? 'plain' : 'link'}
                onClick={() => {
                    if (isPaused) {
                        scrollToBottom()
                    }
                    setIsPaused(!isPaused)
                }}
            >
                {isPaused ? <PlayIcon /> : <PauseIcon />}
                {isPaused ? t('log.resume') : t('log.pause')}
            </Button>
        ),
        [isPaused, scrollToBottom, t]
    )

    const leftAlignedToolbarGroup = React.useMemo(
        () => (
            <>
                <ToolbarToggleGroup toggleIcon={<EllipsisVIcon />} breakpoint='md'>
                    <ToolbarItem variant='search-filter' style={{ minWidth: '280px', maxWidth: '500px' }}>
                        <SWSelect
                            clearable={false}
                            options={dataSources.map(({ id }) => ({ label: id, id }))}
                            value={selectedDataSource ? [{ id: selectedDataSource }] : []}
                            onChange={(params) => {
                                if (!params.option) {
                                    return
                                }
                                reset()
                                const selection = params.option.id
                                setSelectedDataSource(selection as any)
                            }}
                        />
                    </ToolbarItem>
                    <ToolbarItem variant='search-filter'>
                        <LogViewerSearch
                            onChange={(e) => {
                                if ((e.target as any).value?.length === 0) {
                                    setIsPaused(false)
                                    scrollToBottom()
                                }
                            }}
                            onFocus={() => setIsPaused(true)}
                            placeholder='Search'
                            minSearchChars={1}
                        />
                    </ToolbarItem>
                </ToolbarToggleGroup>
                <ToolbarItem>{ControlButton}</ToolbarItem>
            </>
        ),
        [dataSources, selectedDataSource, reset, ControlButton, scrollToBottom]
    )

    const rightAlignedToolbarGroup = React.useMemo(
        () => (
            <>
                <ToolbarGroup variant='icon-button-group'>
                    <ToolbarItem>
                        <Tooltip position='top' content={<div>Top</div>}>
                            <Button onClick={onScrollTop} variant='plain' aria-label='Scroll to TOP'>
                                <HistoryIcon />
                            </Button>
                        </Tooltip>
                    </ToolbarItem>
                    <ToolbarItem>
                        <Tooltip position='top' content={<div>Download</div>}>
                            <Button onClick={onDownloadClick} variant='plain' aria-label='Download current logs'>
                                <DownloadIcon />
                            </Button>
                        </Tooltip>
                    </ToolbarItem>
                    <ToolbarItem>
                        <Tooltip position='top' content={<div>Expand</div>}>
                            <Button onClick={onExpand} variant='plain' aria-label='View log viewer in full screen'>
                                <ExpandIcon />
                            </Button>
                        </Tooltip>
                    </ToolbarItem>
                </ToolbarGroup>
            </>
        ),
        [onDownloadClick, onScrollTop, onExpand]
    )

    const FooterButton = React.useMemo(() => {
        const handleClick = () => {
            setIsPaused(false)
            if (logViewerRef && logViewerRef.current) {
                logViewerRef.current?.scrollToBottom()
            }
        }
        return (
            <Button
                onClick={handleClick}
                isBlock
                style={{
                    visibility: isPaused ? 'visible' : 'hidden',
                }}
            >
                <OutlinedPlayCircleIcon />
                {t('log.resume')} {linesBehind === 0 ? null : t('log.behind.lines', [linesBehind])}
            </Button>
        )
    }, [setIsPaused, linesBehind, isPaused, t])

    return (
        <LogViewer
            data={renderData}
            theme='dark'
            // @ts-ignore
            id='complex-toolbar-demo'
            scrollToRow={currentItemCount}
            innerRef={logViewerRef}
            height='100%'
            style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                minHeight: '500px',
                flex: 'auto',
            }}
            toolbar={
                <Toolbar>
                    <ToolbarContent>
                        <ToolbarGroup alignment={{ default: 'alignLeft' }}>{leftAlignedToolbarGroup}</ToolbarGroup>
                        <ToolbarGroup alignment={{ default: 'alignRight' }}>{rightAlignedToolbarGroup}</ToolbarGroup>
                    </ToolbarContent>
                </Toolbar>
            }
            overScanCount={10}
            footer={isPaused && FooterButton}
            onScroll={onScroll}
        />
    )
}

export default ComplexToolbarLogViewer

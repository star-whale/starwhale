import '@patternfly/react-core/dist/styles/base.css'

import React from 'react'
// import { data } from '../examples/realTestData'
import { LogViewer, LogViewerSearch } from '@patternfly/react-log-viewer'
import {
    Badge,
    Button,
    Select,
    SelectOption,
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
import useWebSocket from '@/hooks/useWebSocket'
import SWSelect, { ISelectProps } from '@starwhale/ui/Select'
import './LogView.scss'

const empty: any[] = []
const RESUME_HEIGHT = 32

function useSourceData({ ws = '', data }: { ws: string; data?: string }) {
    const [content, setContent] = React.useState<any[]>([])

    console.log(ws)

    React.useEffect(() => {
        if (data) {
            setContent(data.split('\n'))
        }
    }, [data])

    useWebSocket({
        wsUrl: ws,
        onMessage: React.useCallback((d) => {
            setContent((prev) => {
                prev.push(d)
                return prev
            })
        }, []),
        onClose: React.useCallback(() => {
            setContent(empty)
        }, []),
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
    const [isFullScreen, setIsFullScreen] = React.useState(false)
    const [itemCount, setItemCount] = React.useState(1)
    const [currentItemCount, setCurrentItemCount] = React.useState(0)
    const [renderData, setRenderData] = React.useState('')
    const [selectedDataSource, setSelectedDataSource] = React.useState(dataSources[0]?.id)
    const [selectDataSourceOpen, setSelectDataSourceOpen] = React.useState(false)
    const [timer, setTimer] = React.useState(null)
    const [buffer, setBuffer] = React.useState([])
    const [linesBehind, setLinesBehind] = React.useState(0)
    const logViewerRef = React.useRef<any>()

    const selectedDataSourceObj = dataSources?.find((d) => d.id === selectedDataSource) || {}
    const { content: selectedData } = useSourceData(selectedDataSourceObj)

    console.log(dataSources)
    console.log(selectedData, selectedDataSourceObj)

    const reset = React.useCallback(() => {
        setLinesBehind(0)
        setBuffer([])
        setItemCount(1)
        setCurrentItemCount(0)
        setSelectDataSourceOpen(false)
        if (logViewerRef && logViewerRef.current) {
            logViewerRef.current?.scrollToBottom()
        }
    }, [logViewerRef])

    React.useEffect(() => {
        reset()
        setSelectedDataSource(dataSources[0]?.id)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [dataSources])

    React.useEffect(() => {
        setItemCount(selectedData.length)
        setBuffer(selectedData)
    }, [selectedData])

    React.useEffect(() => {
        if (!isPaused && buffer.length > 0) {
            setCurrentItemCount(buffer.length)
            setRenderData(buffer.join('\n'))
            if (logViewerRef && logViewerRef.current) {
                logViewerRef.current?.scrollToBottom()
            }
        } else if (buffer.length !== currentItemCount) {
            setLinesBehind(buffer.length - currentItemCount)
        } else {
            setLinesBehind(0)
        }
    }, [isPaused, buffer, currentItemCount])

    const onExpandClick = () => {
        const element = document.querySelector('#complex-toolbar-demo')

        if (!isFullScreen) {
            if (element?.requestFullscreen) {
                element.requestFullscreen()
            } else if (element?.mozRequestFullScreen) {
                element?.mozRequestFullScreen()
            } else if (element?.webkitRequestFullScreen) {
                element?.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT)
            }
            setIsFullScreen(true)
        } else {
            if (document.exitFullscreen) {
                document.exitFullscreen()
            } else if (document.webkitExitFullscreen) {
                /* Safari */
                document.webkitExitFullscreen()
            } else if (document.msExitFullscreen) {
                /* IE11 */
                document.msExitFullscreen()
            }
            setIsFullScreen(false)
        }
    }

    const onDownloadClick = () => {
        const element = document.createElement('a')
        const dataToDownload = [selectedData.join('\n')]
        const file = new Blob(dataToDownload, { type: 'text/plain' })
        element.href = URL.createObjectURL(file)
        element.download = `${selectedDataSource}.txt`
        document.body.appendChild(element)
        element.click()
        document.body.removeChild(element)
    }

    const onScroll = ({ scrollOffsetToBottom, _scrollDirection, scrollUpdateWasRequested }) => {
        if (!scrollUpdateWasRequested) {
            if (scrollOffsetToBottom > 0) {
                setIsPaused(true)
            } else {
                setIsPaused(false)
            }
        }
    }

    const selectDataSourceMenu = dataSources?.map(({ type, id: value }) => (
        <SelectOption
            key={value}
            value={value}
            isSelected={selectedDataSource === value}
            isChecked={selectedDataSource === value}
        >
            <Badge key={value}>{type}</Badge>
            {` ${value}`}
        </SelectOption>
    ))

    const selectDataSourcePlaceholder = (
        <>
            <Badge>{selectedDataSourceObj?.type}</Badge>
            {` ${selectedDataSource}`}
        </>
    )

    const ControlButton = () => (
        <Button
            variant={isPaused ? 'plain' : 'link'}
            onClick={() => {
                setIsPaused(!isPaused)
            }}
        >
            {isPaused ? <PlayIcon /> : <PauseIcon />}
            {isPaused ? ' Resume Log' : ' Pause Log'}
        </Button>
    )

    const leftAlignedToolbarGroup = (
        <>
            <ToolbarToggleGroup toggleIcon={<EllipsisVIcon />} breakpoint='md'>
                <ToolbarItem variant='search-filter' style={{ width: '200px' }}>
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
                            }
                        }}
                        onFocus={() => setIsPaused(true)}
                        placeholder='Search'
                        minSearchChars={1}
                    />
                </ToolbarItem>
            </ToolbarToggleGroup>
            <ToolbarItem>
                <ControlButton />
            </ToolbarItem>
        </>
    )

    const rightAlignedToolbarGroup = (
        <>
            <ToolbarGroup variant='icon-button-group'>
                <ToolbarItem>
                    <Tooltip position='top' content={<div>Download</div>}>
                        <Button onClick={onDownloadClick} variant='plain' aria-label='Download current logs'>
                            <DownloadIcon />
                        </Button>
                    </Tooltip>
                </ToolbarItem>
                <ToolbarItem>
                    <Tooltip position='top' content={<div>Expand</div>}>
                        <Button onClick={onExpandClick} variant='plain' aria-label='View log viewer in full screen'>
                            <ExpandIcon />
                        </Button>
                    </Tooltip>
                </ToolbarItem>
            </ToolbarGroup>
        </>
    )

    const FooterButton = () => {
        const handleClick = (_e) => {
            setIsPaused(false)
        }
        return (
            <Button onClick={handleClick} isBlock style={{}}>
                <OutlinedPlayCircleIcon />
                resume {linesBehind === 0 ? null : `and show ${linesBehind} lines`}
            </Button>
        )
    }
    return (
        <LogViewer
            data={renderData}
            theme='dark'
            id='complex-toolbar-demo'
            scrollToRow={currentItemCount}
            innerRef={logViewerRef}
            height='100%'
            style={{
                flexGrow: 0,
                minHeight: '500px',
                flexBasis: isPaused ? `calc(100% - ${RESUME_HEIGHT}px)` : '100%',
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
            footer={isPaused && <FooterButton />}
            onScroll={onScroll}
        />
    )
}

export default ComplexToolbarLogViewer

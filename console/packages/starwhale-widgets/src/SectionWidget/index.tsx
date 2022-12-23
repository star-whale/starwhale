import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import React, { useEffect, useState } from 'react'
import { Subscription } from 'rxjs'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType } from '@starwhale/core/types'
import { PanelAddEvent, PanelEditEvent, PanelDeleteEvent, PanelPreviewEvent } from '@starwhale/core/events'
import { WidgetPlugin } from '@starwhale/core/widget'
import IconFont from '@starwhale/ui/IconFont'
// @ts-ignore
import { Resizable } from 'react-resizable'
import 'react-resizable/css/styles.css'
import { createUseStyles } from 'react-jss'
import { DragEndEvent, DragStartEvent } from '@starwhale/core/events/common'
import SectionAccordionPanel from './component/SectionAccordionPanel'
import SectionForm from './component/SectionForm'
import ChartConfigGroup from './component/ChartConfigGroup'

const useStyles = createUseStyles({
    panelWrapper: {
        '&  .panel-operator': {
            visibility: 'hidden',
        },
        '&:hover > .panel-operator': {
            visibility: 'visible',
        },
        '& .react-resizable-handle': {
            visibility: 'hidden',
        },
        '&:hover > .react-resizable-handle': {
            visibility: 'visible',
        },
    },
    contentWrapper: {
        width: '100%',
        height: '100%',
        overflow: 'auto',
        padding: '40px 20px 20px',
        backgroundColor: '#fff',
        border: '1px solid #CFD7E6',
        borderRadius: '4px',
        position: 'relative',
    },
    chartGroup: {
        position: 'absolute',
    },
})

export const CONFIG: WidgetConfig = {
    type: 'ui:section',
    name: 'test',
    group: WidgetGroupType.LIST,
    description: 'ui layout for dynamic grid view',
    optionConfig: {
        title: 'Panel',
        isExpaned: true,
        layoutConfig: {
            padding: 20,
            columnsPerPage: 3,
            rowsPerPage: 3,
            boxWidth: 430,
            boxHeight: 274,
        },
        layout: {
            width: 430,
            height: 274,
        },
    },
}

type Option = typeof CONFIG['optionConfig']

// @ts-ignore
function SectionWidget(props: WidgetRendererProps<Option, any>) {
    const styles = useStyles()
    const { optionConfig, children, eventBus, type } = props

    // @ts-ignore
    const { title = '', isExpaned = false, layoutConfig, layout } = optionConfig as Option
    const [isDragging, setIsDragging] = useState(false)

    const len = React.Children.count(children)
    const { boxWidth, boxHeight, padding } = layoutConfig
    const { width, height } = layout

    const [isModelOpen, setIsModelOpen] = useState(false)

    const handleSectionForm = ({ name }: { name: string }) => {
        props.onOptionChange?.({
            title: name,
        })
        setIsModelOpen(false)
    }
    const handleEditPanel = (id: string) => {
        eventBus.publish(new PanelEditEvent({ id }))
    }
    const handleDeletePanel = (id: string) => {
        eventBus.publish(new PanelDeleteEvent({ id }))
    }
    const handlePreviewPanel = (id: string) => {
        eventBus.publish(new PanelPreviewEvent({ id }))
    }
    const handleExpanded = (expanded: boolean) => {
        props.onOptionChange?.({
            isExpaned: expanded,
        })
    }
    const handleLayoutChange = (args: any) => {
        props.onOptionChange?.({
            layout: args,
        })
    }

    // subscription
    useEffect(() => {
        const subscription = new Subscription()
        subscription.add(
            eventBus.getStream(DragStartEvent).subscribe({
                next: () => setIsDragging(true),
            })
        )
        subscription.add(
            eventBus.getStream(DragEndEvent).subscribe({
                next: () => setIsDragging(false),
            })
        )
        return () => {
            subscription.unsubscribe()
        }
    }, [eventBus])

    const [rect, setRect] = useState({ width, height })
    const [resizeRect, setResizeRect] = useState({
        start: false,
        width,
        height,
        left: 0,
        top: 0,
        clientX: 0,
        clientY: 0,
        offsetClientX: 0,
        offsetClientY: 0,
    })
    const previeRef = React.useRef<HTMLDivElement>(null)
    const wrapperRef = React.useRef<HTMLDivElement>(null)

    return (
        <div>
            <SectionAccordionPanel
                childNums={len}
                title={title}
                expanded={isDragging ? false : isExpaned}
                onExpanded={handleExpanded}
                onPanelAdd={() => {
                    // @FIXME abatract events
                    eventBus.publish(
                        new PanelAddEvent({
                            // @ts-ignore
                            path: props.path,
                        })
                    )
                }}
                onSectionRename={() => {
                    setIsModelOpen(true)
                }}
                onSectionAddAbove={() => {
                    props.onLayoutCurrentChange?.({ type }, { type: 'addAbove' })
                }}
                onSectionAddBelow={() => {
                    props.onLayoutCurrentChange?.({ type }, { type: 'addBelow' })
                }}
                onSectionDelete={() => {
                    props.onLayoutCurrentChange?.({ type }, { type: 'delete', id: props.id })
                }}
            >
                {/* eslint-disable-next-line @typescript-eslint/no-use-before-define */}
                {len === 0 && <EmptyPlaceholder />}
                <div
                    ref={previeRef}
                    style={{
                        position: 'absolute',
                        width: `${resizeRect.width + resizeRect.offsetClientX}px`,
                        height: `${resizeRect.height + resizeRect.offsetClientY}px`,
                        transform: `translate(${resizeRect.left}px, ${resizeRect.top}px)`,
                        background: '#ddedfc',
                        border: '1px dashed #338dd8',
                        opacity: '0.5',
                        zIndex: 99,
                        display: resizeRect.start ? 'block' : 'none',
                    }}
                />
                <div
                    ref={wrapperRef}
                    style={{
                        display: 'grid',
                        gap: '10px',
                        gridTemplateColumns: `repeat(auto-fit, minmax(${rect.width}px, 1fr))`,
                        gridAutoRows: `${rect.height}px`,
                        transition: 'all 0.3s',
                        position: 'relative',
                        padding: `${padding}px`,
                    }}
                >
                    {React.Children.map(children as any, (child: React.ReactElement) => (
                        <Resizable
                            width={rect.width}
                            height={rect.height}
                            axis='both'
                            onResizeStart={(e: any) => {
                                const parent = e.target.parentNode
                                const parentRect = parent.getBoundingClientRect()
                                setResizeRect({
                                    start: true,
                                    clientX: e.clientX,
                                    clientY: e.clientY,
                                    width: parentRect.width,
                                    height: parentRect.height,
                                    top: e.target.parentNode.offsetTop,
                                    left: e.target.parentNode.offsetLeft,
                                    offsetClientX: 0,
                                    offsetClientY: 0,
                                })
                                previeRef.current?.focus()
                            }}
                            onResize={(e: any) => {
                                // @ts-ignore
                                const wrapperWidth = wrapperRef.current?.getBoundingClientRect()?.width - padding * 2
                                if (resizeRect.width + e.clientX - resizeRect.clientX < boxWidth) return
                                if (resizeRect.height + e.clientY - resizeRect.clientY < boxHeight) return
                                if (resizeRect.width + e.clientX - resizeRect.clientX > wrapperWidth) return

                                setResizeRect({
                                    ...resizeRect,
                                    offsetClientX: e.clientX - resizeRect.clientX,
                                    offsetClientY: e.clientY - resizeRect.clientY,
                                })
                            }}
                            onResizeStop={() => {
                                const rectTmp = {
                                    width: resizeRect.width + resizeRect.offsetClientX,
                                    height: resizeRect.height + resizeRect.offsetClientY,
                                }
                                handleLayoutChange(rectTmp)
                                setRect(rectTmp)
                                setResizeRect({
                                    ...resizeRect,
                                    start: false,
                                })
                            }}
                        >
                            <div className={styles.panelWrapper} id={child.props.id}>
                                <div className={styles.contentWrapper}>{child}</div>
                                <ChartConfigGroup
                                    onEdit={() => handleEditPanel(child.props.id)}
                                    onDelete={() => handleDeletePanel(child.props?.id)}
                                    onPreview={() => handlePreviewPanel(child.props?.id)}
                                />
                            </div>
                        </Resizable>
                    ))}
                </div>
            </SectionAccordionPanel>
            <Modal isOpen={isModelOpen} onClose={() => setIsModelOpen(false)} closeable animate autoFocus>
                <ModalHeader>Panel</ModalHeader>
                <ModalBody>
                    <SectionForm onSubmit={handleSectionForm} formData={{ name: title }} />
                </ModalBody>
            </Modal>
        </div>
    )
}

const EmptyPlaceholder = () => {
    return (
        <BusyPlaceholder type='center' style={{ minHeight: '240px' }}>
            <IconFont type='emptyChart' size={64} />
            {/* eslint-disable-next-line react/no-unescaped-entities */}
            <span>Click "Add Chart" to add visualizations</span>
        </BusyPlaceholder>
    )
}

// @ts-ignore
const widget = new WidgetPlugin<Option>(SectionWidget, CONFIG)

export default widget

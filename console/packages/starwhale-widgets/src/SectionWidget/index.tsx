import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import React, { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { Subscription } from 'rxjs'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType, WidgetStoreState } from '@starwhale/core/types'
import { PanelChartDownloadEvent, PanelChartReloadEvent } from '@starwhale/core/events'
import { WidgetPlugin } from '@starwhale/core/widget'
import { PanelContextProvider, useEditorContext } from '@starwhale/core/context'
import IconFont from '@starwhale/ui/IconFont'
// @ts-ignore
import { Resizable } from 'react-resizable'
import 'react-resizable/css/styles.css'
import { createUseStyles } from 'react-jss'
import { DragEndEvent, DragStartEvent } from '@starwhale/core/events/common'
import { EvalSectionDeleteEvent } from '@starwhale/core/events/app'
import SectionAccordionPanel from './component/SectionAccordionPanel'
import SectionForm from './component/SectionForm'
import ChartConfigGroup from './component/ChartConfigGroup'
import useTranslation from '@/hooks/useTranslation'
import EvalSelectList from '@/components/Editor/EvalSelectList'
import { EvalSelectDataT } from '@/components/Editor/EvalSelectForm'
import { WidgetFormModal, WidgetFormModel } from '@starwhale/core/form'
import shallow from 'zustand/shallow'
import _ from 'lodash'

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
            backgroundSize: '12px',
            padding: '3px',
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
    contentTitle: {
        position: 'absolute',
        left: '20px',
        top: '16px',
        display: 'flex',
        gap: '6px',
        zIndex: 2,
        fontWeight: '600',
        fontSize: '14px',
        color: ' rgba(2,16,43,0.60);',
    },
})

export const CONFIG: WidgetConfig = {
    type: 'ui:section',
    name: 'test',
    group: WidgetGroupType.LIST,
    description: 'ui layout for dynamic grid view',
    optionConfig: {
        title: '',
        isExpaned: true,
        isEvaluationList: false,
        isEvaluationListShow: true,
        evalSelectData: {} as EvalSelectDataT, // {projectId: {}}
        evalTableCurrentViewData: {},
        layoutConfig: {
            padding: 20,
            columnsPerPage: 3,
            rowsPerPage: 3,
            boxWidth: 330,
            boxHeight: 274,
        },
        layout: {
            width: 430,
            height: 274,
        },
    },
}

type OptionConfig = (typeof CONFIG)['optionConfig']

const selector = (s: WidgetStoreState) => ({
    onLayoutChildrenChange: s.onLayoutChildrenChange,
    onWidgetChange: s.onWidgetChange,
    onWidgetDelete: s.onWidgetDelete,
    panelGroup: s.panelGroup,
    widgets: s.widgets,
    editable: s.editable,
})

// @ts-ignore
function SectionWidget(props: WidgetRendererProps<OptionConfig, any>) {
    const { store } = useEditorContext()
    const api = store(selector, shallow)
    const { editable = true } = api
    const [editWidget, setEditWidget] = useState<{
        type?: string
        path?: any[]
        id?: string
        data?: any
        preview?: boolean
    }>({})
    const [isPanelModalOpen, setIsPanelModalOpen] = React.useState(false)

    const [t] = useTranslation()
    const styles = useStyles()
    const { optionConfig, children, eventBus, type } = props
    const {
        isExpaned = false,
        layoutConfig,
        layout,
        isEvaluationList,
        evalSelectData,
        evalTableCurrentViewData,
        isEvaluationListShow,
    } = optionConfig as any
    const [isDragging, setIsDragging] = useState(false)
    const [isModelOpen, setIsModelOpen] = useState(false)
    const len = children ? React.Children.count(children) : 0
    const { boxWidth, boxHeight, padding } = layoutConfig
    const { width, height } = layout

    const title = optionConfig?.title || t('panel.name')

    const handleSectionForm = ({ name }: { name: string }) => {
        props.onOptionChange?.({
            title: name,
        })
        setIsModelOpen(false)
    }
    const handleDownloadPanel = (id: string) => {
        eventBus.publish(new PanelChartDownloadEvent({ id }))
    }
    const handleReloadPanel = (id: string) => {
        eventBus.publish(new PanelChartReloadEvent({ id }))
    }
    const handleSelectCurrentViewDataChange = (data: any) => {
        props.onOptionChange?.({
            evalTableCurrentViewData: data,
        })
    }
    const handleSelectDataChange = (data: any) => {
        props.onOptionChange?.({
            evalSelectData: data,
        })
    }
    const handleEvaluationListShowChange = (editing: boolean) => {
        props.onOptionChange?.({
            isEvaluationListShow: editing,
        })
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

    const handleSectionAddChart = () => {
        setEditWidget({ path: props.path, id: props.id, type: 'add' })
        setIsPanelModalOpen(true)
    }

    const handleSectionRename = () => {
        setIsModelOpen(true)
    }

    const handleSectionAddAbove = isEvaluationList
        ? undefined
        : () => {
              props.onLayoutCurrentChange?.({ type }, { type: 'addAbove' })
          }

    const handleSectionAddBelow = isEvaluationList
        ? undefined
        : () => {
              props.onLayoutCurrentChange?.({ type }, { type: 'addBelow' })
          }

    const handleSectionDelete = () => {
        props.onLayoutCurrentChange?.({ type }, { type: 'delete', id: props.id })
        if (isEvaluationList) eventBus.publish(new EvalSectionDeleteEvent({ id: props.id }))
    }

    const handleChartAddSave = ({ formData, optionConfig: tmp }: any) => {
        const { path } = editWidget
        if (!path || path.length === 0) return
        api.onLayoutChildrenChange(['tree', ...path], ['tree', ...path, 'children'], {
            type: formData.chartType,
            fieldConfig: {
                data: formData,
            },
            optionConfig: tmp,
        })
    }

    const handleChartEditSave = ({ formData, optionConfig: tmp }: any) => {
        const { id } = editWidget
        api.onWidgetChange(id, {
            type: formData.chartType,
            fieldConfig: {
                data: formData,
            },
            optionConfig: tmp,
        })
    }

    const handelChartDeletePanel = (id: string) => {
        api.onWidgetDelete(id)
    }

    const handleChartPreview = (id: string) => {
        setEditWidget({ id, preview: true })
        setIsPanelModalOpen(true)
    }

    const handleChartEdit = (id: string) => {
        setEditWidget({ id, preview: false })
        setIsPanelModalOpen(true)
    }

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
    const memoChildren = React.useMemo(() => {
        return React.Children.map(children as any, (child: React.ReactElement) => {
            if (!child) return null

            const chartTitle = _.get(api.widgets, [child.props.id, 'fieldConfig', 'data', 'chartTitle'], '')

            return (
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
                        e.stopPropagation()
                    }}
                    onResize={(e: any) => {
                        const wrapperWidth =
                            // @ts-ignore
                            wrapperRef.current?.getBoundingClientRect()?.width - padding * 2
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
                        <div className={styles.contentTitle}>{chartTitle}</div>
                        {editable && (
                            <ChartConfigGroup
                                onEdit={() => handleChartEdit(child.props.id)}
                                onDelete={() => handelChartDeletePanel(child.props?.id)}
                                onPreview={() => handleChartPreview(child.props?.id)}
                                onDownload={() => handleDownloadPanel(child.props?.id)}
                                onReload={() => handleReloadPanel(child.props?.id)}
                            />
                        )}
                    </div>
                </Resizable>
            )
        })
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [children, rect, resizeRect, styles, boxHeight, boxWidth, padding, api.widgets])

    const form = useRef(new WidgetFormModel())
    useEffect(() => {
        form.current.initPanelSchema({
            panelGroup: api.panelGroup,
        })
    }, [t, api.panelGroup, editWidget.id])

    // console.log(evalSelectData)
    useLayoutEffect(() => {
        if (!wrapperRef.current) return
        const wrect = wrapperRef.current.getBoundingClientRect()
        if (wrect.width < rect.width) {
            setRect((prev) => {
                return {
                    ...prev,
                    width: wrect.width - padding * 2,
                }
            })
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    // useIfChanged({
    //     optionConfig,
    //     evalSelectData,
    //     isEvaluationListShow,
    // })

    const isChartAdd = isEvaluationList ? Object.keys(evalSelectData || {}).length > 0 : true

    return (
        <PanelContextProvider value={{ evalSelectData }}>
            <SectionAccordionPanel
                editable={editable}
                childNums={len}
                title={title}
                expanded={isDragging ? false : isExpaned}
                onExpanded={handleExpanded}
                onPanelChartAdd={isChartAdd && handleSectionAddChart}
                onSectionRename={handleSectionRename}
                onSectionAddAbove={handleSectionAddAbove}
                onSectionAddBelow={handleSectionAddBelow}
                onSectionDelete={handleSectionDelete}
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
                    {memoChildren}
                </div>
                {isEvaluationList && (
                    <div className='mx-20px'>
                        <EvalSelectList
                            editing={isEvaluationListShow}
                            onEditingChange={handleEvaluationListShowChange}
                            currentView={evalTableCurrentViewData}
                            onCurrentViewChange={handleSelectCurrentViewDataChange}
                            value={evalSelectData}
                            onSelectDataChange={handleSelectDataChange}
                        />
                    </div>
                )}
            </SectionAccordionPanel>
            <Modal
                isOpen={isModelOpen}
                onClose={() => setIsModelOpen(false)}
                closeable
                animate
                autoFocus
                returnFocus={false}
            >
                <ModalHeader>{t('panel.name')}</ModalHeader>
                <ModalBody>
                    <SectionForm onSubmit={handleSectionForm} formData={{ name: title }} />
                </ModalBody>
            </Modal>
            <WidgetFormModal
                form={form.current}
                id={editWidget.id}
                payload={editWidget}
                isShow={isPanelModalOpen}
                setIsShow={(bool) => {
                    setIsPanelModalOpen(bool)
                    if (bool) return
                    setEditWidget({})
                }}
                store={store}
                onFormSubmit={(data: any) => {
                    if (editWidget?.type === 'add') {
                        handleChartAddSave(data)
                    } else {
                        handleChartEditSave(data)
                    }
                    setIsPanelModalOpen(false)
                    setEditWidget({})
                }}
            />
        </PanelContextProvider>
    )
}

const EmptyPlaceholder = () => {
    const [t] = useTranslation()

    return (
        <BusyPlaceholder type='center' style={{ minHeight: '240px' }}>
            <IconFont type='emptyChart' size={64} />
            <span>{t('panel.list.placeholder')}</span>
        </BusyPlaceholder>
    )
}

// @ts-ignore
const widget = new WidgetPlugin<Option>(SectionWidget, CONFIG)

export default widget

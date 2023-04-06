import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import React, { useEffect, useMemo, useState } from 'react'
import { Subscription } from 'rxjs'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType } from '@starwhale/core/types'
import { PanelAddEvent, PanelEditEvent, PanelDeleteEvent, PanelPreviewEvent } from '@starwhale/core/events'
import { WidgetPlugin } from '@starwhale/core/widget'
import IconFont from '@starwhale/ui/IconFont'
import { DragEndEvent, DragStartEvent } from '@starwhale/core/events/common'
import { GridLayout } from './component/GridBasicLayout'
import SectionAccordionPanel from './component/SectionAccordionPanel'
import SectionForm from './component/SectionForm'
import ChartConfigGroup from './component/ChartConfigGroup'
import useTranslation from '@/hooks/useTranslation'

export const CONFIG: WidgetConfig = {
    type: 'ui:section',
    name: 'test',
    group: WidgetGroupType.LIST,
    description: 'ui layout for dynamic grid view',
    optionConfig: {
        title: 'Panel',
        isExpaned: true,
        layoutConfig: {
            gutter: 10,
            columnsPerPage: 3,
            rowsPerPage: 3,
            boxWidth: 430,
            heightWidth: 274,
        },
        gridLayoutConfig: {
            item: {
                w: 1,
                h: 2,
                minW: 1,
                maxW: 3,
                minH: 1,
                maxH: 3,
                isBounded: true,
            },
            cols: 3,
        },
        gridLayout: [],
    },
}

type Option = typeof CONFIG['optionConfig']

// @ts-ignore
function SectionWidget(props: WidgetRendererProps<Option, any>) {
    const { optionConfig, children, eventBus, type } = props

    // @ts-ignore
    const { title = '', isExpaned = false, gridLayoutConfig, gridLayout } = optionConfig as Option
    const [isDragging, setIsDragging] = useState(false)

    const len = React.Children.count(children)
    const { cols } = gridLayoutConfig
    const layout = useMemo(() => {
        if (gridLayout.length !== 0) return gridLayout
        return new Array(len).fill(0).map((_, i) => ({
            i: String(i),
            x: i,
            y: 0,
            ...gridLayoutConfig.item,
        }))
    }, [gridLayout, gridLayoutConfig, len])

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
            gridLayout: args,
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
                <GridLayout
                    rowHeight={300}
                    className='layout'
                    cols={cols}
                    layout={layout}
                    onLayoutChange={handleLayoutChange}
                    containerPadding={[20, 0]}
                    margin={[20, 20]}
                >
                    {/* @ts-ignore */}
                    {React.Children.map(children, (child: React.ReactElement) => (
                        <div key={String(child.props.id)}>
                            <div
                                style={{
                                    width: '100%',
                                    height: '100%',
                                    overflow: 'auto',
                                    padding: '40px 20px 20px',
                                    backgroundColor: '#fff',
                                    border: '1px solid #CFD7E6',
                                    borderRadius: '4px',
                                    position: 'relative',
                                }}
                            >
                                {child}
                                <ChartConfigGroup
                                    onEdit={() => handleEditPanel(child.props.id)}
                                    onDelete={() => handleDeletePanel(child.props?.id)}
                                    onPreview={() => handlePreviewPanel(child.props?.id)}
                                />
                            </div>
                        </div>
                    ))}
                </GridLayout>
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

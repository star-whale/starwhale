import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import React, { useEffect, useMemo, useState } from 'react'
import Button from '@starwhale/ui/Button'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType } from '@starwhale/core/types'
import { GridLayout } from './component/GridBasicLayout'
import SectionAccordionPanel from './component/SectionAccordionPanel'
import SectionForm from './component/SectionForm'
import { PanelAddEvent, PanelEditEvent } from '@starwhale/core/events'
import { WidgetPlugin } from '@starwhale/core/widget'
import IconFont from '@starwhale/ui/IconFont'
import { DragEndEvent, DragStartEvent } from '../../../starwhale-core/src/events/common'
import { Subscription } from 'rxjs'

export const CONFIG: WidgetConfig = {
    type: 'ui:section',
    name: 'test',
    group: WidgetGroupType.LIST,
    description: 'ui layout for dynamic grid view',
    optionConfig: {
        title: 'Section',
        isExpaned: true,
        layoutConfig: {
            gutter: 10,
            columnsPerPage: 3,
            rowsPerPage: 2,
            boxWidth: 430,
            heightWidth: 274,
        },
        gridLayoutConfig: {
            item: {
                w: 1,
                h: 2,
                minW: 1,
                maxW: 2,
                maxH: 3,
                minH: 1,
                isBounded: true,
            },
            cols: 2,
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
                next: (evt) => setIsDragging(true),
            })
        )
        subscription.add(
            eventBus.getStream(DragEndEvent).subscribe({
                next: (evt) => setIsDragging(false),
            })
        )
        return () => {
            subscription.unsubscribe()
        }
    }, [])

    return (
        <div>
            <SectionAccordionPanel
                childNums={len}
                title={title}
                expanded={isDragging ? false : isExpaned}
                onExpanded={handleExpanded}
                onPanelAdd={() => {
                    console.log('add panel')
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
                {len === 0 && <BusyPlaceholder type='empty' style={{ minHeight: '240px' }} />}
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
                    {React.Children.map(children, (child: React.ReactChild, i: number) => (
                        <div
                            key={i}
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
                            <div
                                style={{
                                    position: 'absolute',
                                    right: '20px',
                                    top: '16px',
                                }}
                            >
                                <Button
                                    // @FIXME direct used child props here ?
                                    // @ts-ignore
                                    onClick={() => handleEditPanel(child.props.id)}
                                    size='compact'
                                    kind='secondary'
                                    overrides={{
                                        BaseButton: {
                                            style: {
                                                'display': 'flex',
                                                'fontSize': '12px',
                                                'backgroundColor': '#F4F5F7',
                                                'width': '20px',
                                                'height': '20px',
                                                'textDecoration': 'none',
                                                'color': 'gray !important',
                                                'paddingLeft': '10px',
                                                'paddingRight': '10px',
                                                ':hover span': {
                                                    color: ' #5181E0  !important',
                                                },
                                                ':hover': {
                                                    backgroundColor: '#F0F4FF',
                                                },
                                            },
                                        },
                                    }}
                                >
                                    <IconFont type='edit' size={10} />
                                </Button>
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

// @ts-ignore
const widget = new WidgetPlugin<Option>(SectionWidget, CONFIG)

export default widget

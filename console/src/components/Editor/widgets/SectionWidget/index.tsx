import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import React, { useMemo, useState } from 'react'
import IconFont from '@/components/IconFont'
import Button from '@/components/Button'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { WidgetRendererProps } from '../../Widget/const'
import WidgetPlugin from '../../Widget/WidgetPlugin'
import { GridLayout } from './component/GridBasicLayout'
import SectionAccordionPanel from './component/SectionAccordionPanel'
import SectionForm from './component/SectionForm'

export const CONFIG = {
    type: 'ui:section',
    name: 'test',
    group: 'section',
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

function SectionWidget(props: WidgetRendererProps<Option, any>) {
    const { optionConfig, children, eventBus, type } = props
    const { title = '', isExpaned = false, gridLayoutConfig, gridLayout } = optionConfig as Option

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
        eventBus.publish({
            type: 'edit-panel',
            payload: {
                id,
            },
        })
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

    return (
        <div>
            <SectionAccordionPanel
                childNums={len}
                title={title}
                expanded={isExpaned}
                onExpanded={handleExpanded}
                onPanelAdd={() =>
                    // @FIXME abatract events
                    eventBus.publish({
                        type: 'add-panel',
                        payload: {
                            path: props.path,
                        },
                    })
                }
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
                    {children?.map((child: React.ReactChild, i: number) => (
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

const widget = new WidgetPlugin<Option>(SectionWidget, CONFIG)

export default widget

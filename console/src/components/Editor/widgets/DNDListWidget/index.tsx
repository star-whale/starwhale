import Button from '@/components/Button'
import React from 'react'
import { WidgetConfig, WidgetRendererProps } from '../../Widget/const'
import WidgetPlugin from '../../Widget/WidgetPlugin'

export const CONFIG: WidgetConfig = {
    type: 'ui:dndList',
    name: 'Dragging Section',
    group: 'layout',
}

function DNDListWidget(props: WidgetRendererProps) {
    console.log('DNDListWidget', props)
    const { onOrderChange, onOptionChange, onChildrenAdd, eventBus, children, ...rest } = props
    // if (rest.children?.length === 0 || 1)
    //     return (
    //         <div
    //             style={{
    //                 display: 'flex',
    //                 flexDirection: 'column',
    //                 alignItems: 'center',
    //                 justifyContent: 'center',
    //                 gap: 8,
    //                 height: 300,
    //             }}
    //         >
    //             <BusyPlaceholder type='empty' />
    //         </div>
    //     )
    return (
        <div style={{ width: '100%', height: '100%' }}>
            {children}
            {/* <DnDContainer
                data={children?.map((child, i) => {
                    return {
                        id: i,
                        text: child,
                    }
                })}
            /> */}
            {/* <DNDList {...rest} onChange={onOrderChange} onOptionChange={onOptionChange}>
                {children}
            </DNDList> */}
            <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <Button
                    onClick={() =>
                        eventBus.publish({
                            type: 'save',
                        })
                    }
                >
                    Save
                </Button>
                <Button
                    onClick={() =>
                        eventBus.publish({
                            type: 'add-section',
                            payload: {
                                path: props.path,
                                // @FIXME type const shouldn't be here
                                type: 'ui:section',
                            },
                        })
                    }
                >
                    Add Section
                </Button>
            </div>
        </div>
    )
}

// @FIXME type error
const widget = new WidgetPlugin(DNDListWidget, CONFIG)

export default widget

import Button from '@starwhale/ui/Button'
import React from 'react'
import { PanelSaveEvent, SectionAddEvent } from '@starwhale/core/events'
import { WidgetConfig, WidgetRendererProps, WidgetGroupType } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'

import a from '@starwhale/ui'

export const CONFIG: WidgetConfig = {
    type: 'ui:dndList',
    name: 'Dragging Section',
    group: WidgetGroupType.LIST,
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
            <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <Button onClick={() => eventBus.publish(new PanelSaveEvent())}>Save</Button>
                <Button
                    onClick={() =>
                        eventBus.publish(
                            new SectionAddEvent({
                                path: props.path,
                                // @FIXME type const shouldn't be here
                                type: 'ui:section',
                            })
                        )
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

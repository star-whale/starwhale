import Button from '@starwhale/ui/Button'
import React, { useState } from 'react'
import { DragStartEvent, PanelSaveEvent, SectionAddEvent } from '@starwhale/core/events'
import { WidgetConfig, WidgetRendererProps, WidgetGroupType } from '@starwhale/core/types'
import WidgetPlugin from '@starwhale/core/widget/WidgetPlugin'
import { ReactSortable } from 'react-sortablejs'
import IconFont from '@/components/IconFont'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { createUseStyles } from 'react-jss'
import { DragEndEvent } from '../../../starwhale-core/src/events/common'

export const CONFIG: WidgetConfig = {
    type: 'ui:dndList',
    name: 'Dragging Section',
    group: WidgetGroupType.LIST,
}

const useStyles = createUseStyles({
    empty: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        height: 300,
    },
    wrapper: {
        'position': 'relative',
        'width': '100%',
        '&:hover': {
            '& $handler': {
                display: 'block',
            },
        },
        '& .sortable-drag': {
            height: '50px',
        },
    },
    handler: {
        position: 'absolute',
        top: '15px',
        width: '30px',
        marginLeft: '-15px',
        left: '50%',
        display: 'none',
        cursor: 'pointer',
        textAlign: 'center',
    },
})

function DNDListWidget(props: WidgetRendererProps) {
    const styles = useStyles()
    // console.log('DNDListWidget', props)

    // @ts-ignore
    const { onOrderChange, onOptionChange, onChildrenAdd, eventBus, children, ...rest } = props
    if (React.Children.count(children) === 0)
        return (
            <div className={styles.empty}>
                <BusyPlaceholder type='empty' />
            </div>
        )
    const items =
        React.Children.map(children, (child, i) => ({
            id: i,
            child,
        })) ?? []
    const [state, setState] = useState<any[]>(items)

    console.log(state)

    return (
        <div style={{ width: '100%', height: '100%' }}>
            <ReactSortable
                list={state}
                setList={setState}
                animation={200}
                onStart={(props) => {
                    console.log('DragStartEvent', props)
                    eventBus.publish(new DragStartEvent())
                }}
                onEnd={(props) => {
                    console.log('DragEndEvent', props)
                    eventBus.publish(new DragEndEvent())
                }}
            >
                {state?.map((item) => {
                    return (
                        // style={{ height: item.chosen ? '50px' : 'auto' }}
                        <div key={item.id} className={styles.wrapper}>
                            <div className={`handle ${styles.handler}`}>
                                <IconFont type='drag' />
                            </div>
                            {item.child}
                        </div>
                    )
                })}
            </ReactSortable>
            <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <Button onClick={() => eventBus.publish(new PanelSaveEvent())}>Save</Button>
                <Button
                    onClick={() =>
                        eventBus.publish(
                            new SectionAddEvent({
                                // @ts-ignore
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

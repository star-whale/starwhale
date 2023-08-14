import Button from '@starwhale/ui/Button'
import React, { useEffect, useRef, useState } from 'react'
import { DragStartEvent, PanelChartSaveEvent, SectionAddEvent } from '@starwhale/core/events'
import { WidgetConfig, WidgetRendererProps, WidgetGroupType } from '@starwhale/core/types'
import WidgetPlugin from '@starwhale/core/widget/WidgetPlugin'
import { ReactSortable } from 'react-sortablejs'
import IconFont from '@starwhale/ui/IconFont'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import { createUseStyles } from 'react-jss'
import { DragEndEvent } from '@starwhale/core/events/common'
import { WithCurrentAuth } from '@/api/WithAuth'
import useTranslation from '@/hooks/useTranslation'

export const CONFIG: WidgetConfig = {
    type: 'ui:dndList',
    name: 'Dragging Section',
    group: WidgetGroupType.LIST,
    optionConfig: {
        isSave: true,
        isAddPanel: true,
    },
}

const useStyles = createUseStyles({
    dndList: {
        'width': '100%',
        'height': '100%',
        'backgroundColor': '#fafbfc',
        '& .sortable-ghost ': {
            height: '50px',
            overflow: 'hidden',
            boxShadow: ' 0 2px 8px 0 rgba(0,0,0,0.20)',
        },
    },
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
    },
    handler: {
        position: 'absolute',
        top: '15px',
        width: '30px',
        marginLeft: '-15px',
        left: '50%',
        display: 'none',
        // cursor: 'pointer',
        textAlign: 'center',
        // cursor: 'move',
        cursor: '-webkit-grabbing',
    },
})

const ITEM_HEADER_HEIGHT = 48

function DNDListWidget(props: WidgetRendererProps) {
    const styles = useStyles()

    const { onLayoutOrderChange, eventBus, children, optionConfig = {} } = props
    const { isSave, isAddPanel } = optionConfig

    const [state, setState] = useState<any[]>([])

    useEffect(() => {
        setState(
            React.Children.map(children, (child, i) => ({
                // @ts-ignore
                id: child?.props?.id ?? i,
                child,
            })) ?? []
        )
    }, [children])

    const ref = useRef<HTMLDivElement>(null)
    const [isDragging, setIsDragging] = useState(false)
    const [dragContentRect, setDragContentRect] = useState<{ top: number; parent?: DOMRect }>({
        top: 0,
        parent: undefined,
    })

    const calcContent = (itemIndex = 0) => {
        if (ref.current) {
            const rects = Array.from(ref.current.querySelectorAll('.item')).map((child) =>
                child.getBoundingClientRect()
            )
            return {
                parent: ref.current.getBoundingClientRect(),
                top: rects.reduce((prev, current, currentIndex) => {
                    if (currentIndex < itemIndex) return prev + current.height - ITEM_HEADER_HEIGHT
                    return prev
                }, 0),
            }
        }
        return {
            top: 0,
        }
    }
    const dragSelect = (currentIndex: number) => {
        setIsDragging(true)
        setDragContentRect(calcContent(currentIndex))
        eventBus.publish(new DragStartEvent())
    }
    const dragUnselect = () => {
        setIsDragging(false)
        setDragContentRect({ top: 0 })
        eventBus.publish(new DragEndEvent())
    }
    const dragStart = () => {
        eventBus.publish(new DragStartEvent())
    }
    const dragEnd = () => {
        eventBus.publish(new DragEndEvent())
        onLayoutOrderChange?.(state)
    }
    const [t] = useTranslation()

    if (React.Children.count(children) === 0)
        return (
            <div className={styles.empty}>
                <BusyPlaceholder type='empty' />
            </div>
        )

    return (
        <div
            ref={ref}
            data-type='dndlist-widget'
            style={{
                width: '100%',
                height: isDragging ? `${dragContentRect.parent?.height}px` : '100%',
                display: 'flex',
                flexDirection: 'column',
                flex: 1,
                minWidth: 0,
                overflow: 'auto',
                paddingBottom: '30px',
            }}
        >
            <div style={{ flexBasis: isDragging ? dragContentRect.top : 0 }} />
            <ReactSortable
                delay={100}
                handle='.handle'
                list={state}
                setList={setState}
                animation={50}
                onChoose={(args) => {
                    dragSelect(args.oldIndex as number)
                    args.stopPropagation()
                }}
                onUnchoose={dragUnselect}
                onStart={dragStart}
                onEnd={dragEnd}
            >
                {state?.map((item) => {
                    return (
                        <div
                            key={item.id}
                            className={`${styles.wrapper} item`}
                            style={{
                                boxShadow: item.chosen ? '0 2px 8px 0 rgba(0,0,0,0.20)' : undefined,
                                zIndex: item.chosen ? 10 : 0,
                            }}
                        >
                            <div className={`handle ${styles.handler}`}>
                                <IconFont type='drag' />
                            </div>
                            {item.child}
                        </div>
                    )
                })}
            </ReactSortable>
            <div style={{ flex: 1 }} />
            <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                {isSave && (
                    <WithCurrentAuth id='evaluation.panel.save'>
                        <Button onClick={() => eventBus.publish(new PanelChartSaveEvent())}>{t('panel.save')}</Button>
                    </WithCurrentAuth>
                )}

                {isAddPanel && (
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
                        {t('panel.add')}
                    </Button>
                )}
            </div>
        </div>
    )
}

// @FIXME type error
const widget = new WidgetPlugin(DNDListWidget, CONFIG)

export default widget

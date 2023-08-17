import React, { useEffect, useMemo } from 'react'
import deepEqual from 'fast-deep-equal'
import { Subscription } from 'rxjs'
import { useEditorContext } from '../context/EditorContextProvider'
import withWidgetDynamicProps from './withWidgetDynamicProps'
import { WidgetRenderer } from './WidgetRenderer'
import { WidgetProps, WidgetStateT, WidgetTreeNode } from '../types'
import { PanelChartSaveEvent, SectionAddEvent } from '../events/app'
import useRestoreState from './hooks/useRestoreState'
import shallow from 'zustand/shallow'

export const WrapedWidgetNode = withWidgetDynamicProps(function WidgetNode(props: WidgetProps) {
    const { childWidgets, path = [] } = props
    return (
        <WidgetRenderer {...props}>
            {childWidgets &&
                childWidgets.length > 0 &&
                childWidgets.map((node, i) => {
                    const { children: childChildren, id, ...childRest } = node ?? {}
                    return (
                        <WrapedWidgetNode
                            key={id ?? i}
                            id={id}
                            path={[...path, 'children', i]}
                            childWidgets={childChildren}
                            {...childRest}
                        />
                    )
                })}
        </WidgetRenderer>
    )
})

const selector = (s: any) => ({
    onLayoutChildrenChange: s.onLayoutChildrenChange,
    onWidgetChange: s.onWidgetChange,
    onWidgetDelete: s.onWidgetDelete,
})

export type WidgetRenderTreePropsT = {
    initialState?: any
    onSave?: (state: WidgetStateT) => void
}

export function WidgetRenderTree({ initialState, onSave }: WidgetRenderTreePropsT) {
    const { store, eventBus, dynamicVars } = useEditorContext()
    const api = store(selector, shallow)
    const tree = store((state) => state.tree, deepEqual)

    const { toSave } = useRestoreState(store, initialState, dynamicVars)

    // @ts-ignore
    const handleAddSection = ({ path, type }) => {
        api.onLayoutChildrenChange(['tree', ...path], ['tree', ...path, 'children'], {
            type,
        })
    }

    // subscription
    useEffect(() => {
        const subscription = new Subscription()
        subscription.add(
            eventBus.getStream(SectionAddEvent).subscribe({
                next: (evt) => {
                    handleAddSection(evt.payload)
                },
            })
        )
        subscription.add(
            eventBus.getStream(PanelChartSaveEvent).subscribe({
                next: async () => {
                    onSave?.(toSave())
                },
            })
        )
        return () => {
            subscription.unsubscribe()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [store, eventBus])

    const Nodes = useMemo(() => {
        return tree.map((node: WidgetTreeNode, i: number) => (
            <WrapedWidgetNode
                key={node.id ?? i}
                id={node.id}
                type={node.type}
                path={[i]}
                childWidgets={node.children}
            />
        ))
    }, [tree])

    return <div>{Nodes}</div>
}

export default WidgetRenderTree

import React, { useEffect, useMemo } from 'react'
import { Subscription } from 'rxjs'
import { useEditorContext } from '../context/EditorContextProvider'
import withWidgetDynamicProps from './withWidgetDynamicProps'
import { WidgetRenderer } from './WidgetRenderer'
import { WidgetProps, WidgetTreeNode } from '../types'
import { EvalSectionDeleteEvent, PanelChartSaveEvent, SectionAddEvent, SectionAppendEvent } from '../events/app'
import useRestoreState from './hooks/useRestoreState'
import shallow from 'zustand/shallow'
import { useStoreApi } from '../store'
import useOnInitHandler from './hooks/useOnInitHandler'

export const WrapedWidgetNode = withWidgetDynamicProps(function WidgetNode(props: WidgetProps) {
    const { childWidgets, path = [] } = props

    const $child = React.useMemo(() => {
        if (!childWidgets) return null

        return childWidgets.map((node, i) => {
            const { children: childChildren, id, ...childRest } = node || {}
            return (
                <WrapedWidgetNode
                    key={id ?? i}
                    id={id}
                    path={[...path, 'children', i]}
                    childWidgets={childChildren}
                    {...childRest}
                />
            )
        })
    }, [childWidgets, path])

    return <WidgetRenderer {...props}>{$child}</WidgetRenderer>
})

const selector = (s: any) => ({
    onLayoutChildrenChange: s.onLayoutChildrenChange,
    onWidgetChange: s.onWidgetChange,
    onWidgetDelete: s.onWidgetDelete,
    tree: s.tree,
    onInit: s.onInit,
})

export function WidgetRenderTree() {
    const { store, eventBus, dynamicVars } = useEditorContext()
    const api = store(selector, shallow)
    const { tree, onInit } = api
    const storeApi = useStoreApi()
    useOnInitHandler(onInit, { store, eventBus })

    const { toSave } = useRestoreState(dynamicVars)

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
            eventBus.getStream(SectionAppendEvent).subscribe({
                next: () => {
                    handleAddSection({
                        path: [0],
                        type: 'ui:section',
                    })
                },
            })
        )
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
                    storeApi.getState()?.onSave?.(toSave() as any)
                },
            })
        )
        subscription.add(
            eventBus.getStream(EvalSectionDeleteEvent).subscribe({
                next: async () => {
                    storeApi.getState()?.onEvalSectionDelete?.()
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

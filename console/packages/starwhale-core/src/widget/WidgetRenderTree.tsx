import React, { useEffect, useMemo, useRef, useState } from 'react'
import deepEqual from 'fast-deep-equal'
import { Subscription } from 'rxjs'
import { useEditorContext } from '../context/EditorContextProvider'
import withWidgetDynamicProps from './withWidgetDynamicProps'
import { WidgetRenderer } from './WidgetRenderer'
import WidgetFormModel from '../form/WidgetFormModel'
import { WidgetProps, WidgetStateT, WidgetTreeNode } from '../types'
import { BusEventType } from '../events/types'
import {
    PanelChartAddEvent,
    PanelChartDeleteEvent,
    PanelChartEditEvent,
    PanelChartPreviewEvent,
    PanelChartSaveEvent,
    SectionAddEvent,
} from '../events/app'
import WidgetFormModal from '../form/WidgetFormModal'
import WidgetPreviewModal from '../form/WidgetPreviewModal'
import useRestoreState from './hooks/useRestoreState'
import shallow from 'zustand/shallow'
import useTranslation from '@/hooks/useTranslation'

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
    // @ts-ignore
    const [editWidget, setEditWidget] = useState<BusEventType>(null)
    const [isPanelModalOpen, setIsPanelModalOpen] = React.useState(false)
    const [viewWidget, setViewWidget] = useState<PanelChartPreviewEvent>()
    const [isPanelPreviewModalOpen, setIsPanelPreviewModalOpen] = React.useState(false)
    const form = useRef(new WidgetFormModel())

    // @FIXME useTranslation
    const [t] = useTranslation()
    useEffect(() => {
        form.current.initPanelSchema()
    }, [t])

    const { toSave } = useRestoreState(store, initialState, dynamicVars)

    // @ts-ignore
    const handleAddSection = ({ path, type }) => {
        api.onLayoutChildrenChange(['tree', ...path], ['tree', ...path, 'children'], {
            type,
        })
    }

    const handleChartAddPanel = (formData: any) => {
        const { path } = editWidget?.payload
        if (path && path.length > 0)
            api.onLayoutChildrenChange(['tree', ...path], ['tree', ...path, 'children'], {
                type: formData.chartType,
                fieldConfig: {
                    data: formData,
                },
            })
    }

    const handleChartEditPanel = (formData: any) => {
        const { id } = editWidget?.payload
        api.onWidgetChange(id, {
            type: formData.chartType,
            fieldConfig: {
                data: formData,
            },
        })
    }

    const handelChartDeletePanel = (evt: PanelChartDeleteEvent) => {
        const { id } = evt?.payload
        api.onWidgetDelete(id)
    }

    const actions = {
        [PanelChartAddEvent.type]: handleChartAddPanel,
        [PanelChartEditEvent.type]: handleChartEditPanel,
    }

    // subscription
    useEffect(() => {
        const subscription = new Subscription()
        subscription.add(
            eventBus.getStream(PanelChartAddEvent).subscribe({
                next: (evt) => {
                    setIsPanelModalOpen(true)
                    setEditWidget(evt)
                },
            })
        )
        subscription.add(
            eventBus.getStream(PanelChartEditEvent).subscribe({
                next: (evt) => {
                    setIsPanelModalOpen(true)
                    setEditWidget(evt)
                },
            })
        )
        subscription.add(
            eventBus.getStream(PanelChartDeleteEvent).subscribe({
                next: handelChartDeletePanel,
            })
        )
        subscription.add(
            eventBus.getStream(PanelChartPreviewEvent).subscribe({
                next: (evt) => {
                    setIsPanelPreviewModalOpen(true)
                    setViewWidget(evt)
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

    return (
        <div>
            {Nodes}
            <WidgetFormModal
                form={form.current}
                id={editWidget?.payload?.id}
                payload={editWidget?.payload}
                isShow={isPanelModalOpen}
                setIsShow={setIsPanelModalOpen}
                store={store}
                handleFormSubmit={({ formData }: any) => {
                    // @ts-ignore
                    actions[editWidget?.type]?.(formData)
                    setIsPanelModalOpen(false)
                }}
            />
            <WidgetPreviewModal
                id={viewWidget?.payload?.id}
                isShow={isPanelPreviewModalOpen}
                setIsShow={setIsPanelPreviewModalOpen}
                store={store}
            />
        </div>
    )
}

export default WidgetRenderTree

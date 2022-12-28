import React, { useEffect, useMemo, useState } from 'react'
import deepEqual from 'fast-deep-equal'
import { Subscription } from 'rxjs'

// @FIXME move out
// eslint-disable-next-line import/no-extraneous-dependencies
import { updatePanelSetting } from '@/domain/panel/services/panel'
// eslint-disable-next-line import/no-extraneous-dependencies
import { useFetchPanelSetting } from '@/domain/panel/hooks/useSettings'

import { toaster } from 'baseui/toast'
import _ from 'lodash'
import produce from 'immer'
import { useEditorContext } from '../context/EditorContextProvider'
import withWidgetDynamicProps from './withWidgetDynamicProps'
import { WidgetRenderer } from './WidgetRenderer'
import WidgetFormModel from '../form/WidgetFormModel'
import { WidgetProps } from '../types'
import { PanelAddEvent } from '../events'
import { BusEventType } from '../events/types'
import { PanelDeleteEvent, PanelEditEvent, PanelPreviewEvent, PanelSaveEvent, SectionAddEvent } from '../events/app'
import { PANEL_DYNAMIC_MATCHES, replacer } from '../utils/replacer'
import WidgetFormModal from '../form/WidgetFormModal'
import WidgetPreviewModal from '../form/WidgetPreviewModal'
import { useDeepEffect } from '../utils/useDeepEffects'

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

export function WidgetRenderTree() {
    const { store, eventBus, dynamicVars } = useEditorContext()
    const { storeKey: key, projectId } = dynamicVars
    const api = store()
    const tree = store((state) => state.tree, deepEqual)
    // @ts-ignore
    const [editWidget, setEditWidget] = useState<BusEventType>(null)
    const [isPanelModalOpen, setisPanelModalOpen] = React.useState(false)
    const [viewWidget, setViewWidget] = useState<PanelPreviewEvent>()
    const [isPanelPreviewModalOpen, setisPanelPreviewModalOpen] = React.useState(false)

    // @ts-ignore
    const handleAddSection = ({ path, type }) => {
        api.onLayoutChildrenChange(['tree', ...path], ['tree', ...path, 'children'], {
            type,
        })
    }

    const handleAddPanel = (formData: any) => {
        const { path } = editWidget?.payload
        if (path && path.length > 0)
            api.onLayoutChildrenChange(['tree', ...path], ['tree', ...path, 'children'], {
                type: formData.chartType,
                fieldConfig: {
                    data: formData,
                },
            })
    }

    const handleEditPanel = (formData: any) => {
        const { id } = editWidget?.payload
        api.onWidgetChange(id, {
            type: formData.chartType,
            fieldConfig: {
                data: formData,
            },
        })
    }

    const handelDeletePanel = (evt: PanelDeleteEvent) => {
        const { id } = evt?.payload
        api.onWidgetDelete(id)
    }

    const actions = {
        [PanelAddEvent.type]: handleAddPanel,
        [PanelEditEvent.type]: handleEditPanel,
    }

    // use  api store
    // @FIXME refactor load/save, now only global inject what about table row inject ?
    const setting = useFetchPanelSetting(projectId, key)
    useDeepEffect(() => {
        // @FIXME make sure dynamicVars to be exists!
        if (setting.data && dynamicVars?.prefix) {
            try {
                const data = JSON.parse(setting.data)
                Object.keys(data?.widgets).forEach((id) => {
                    _.set(data.widgets, id, replacer(PANEL_DYNAMIC_MATCHES).toOrigin(data.widgets[id], dynamicVars))
                })
                if (store.getState().time < data?.time) store.setState(data)
            } catch (e) {
                // eslint-disable-next-line no-console
                console.log(e)
            }
        }
    }, [setting.data, dynamicVars?.prefix])

    // subscription
    useEffect(() => {
        const subscription = new Subscription()
        subscription.add(
            eventBus.getStream(PanelAddEvent).subscribe({
                next: (evt) => {
                    setisPanelModalOpen(true)
                    setEditWidget(evt)
                },
            })
        )
        subscription.add(
            eventBus.getStream(PanelEditEvent).subscribe({
                next: (evt) => {
                    setisPanelModalOpen(true)
                    setEditWidget(evt)
                },
            })
        )
        subscription.add(
            eventBus.getStream(PanelDeleteEvent).subscribe({
                next: (evt) => handelDeletePanel(evt),
            })
        )
        subscription.add(
            eventBus.getStream(PanelPreviewEvent).subscribe({
                next: (evt) => {
                    setisPanelPreviewModalOpen(true)
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
            eventBus.getStream(PanelSaveEvent).subscribe({
                next: async () => {
                    store.setState({
                        key,
                        time: Date.now(),
                    })
                    let data = store.getState()
                    if (!key) return
                    Object.keys(data?.widgets).forEach((id) => {
                        data = produce(data, (temp) => {
                            _.set(temp.widgets, id, replacer(PANEL_DYNAMIC_MATCHES).toTemplate(temp.widgets[id]))
                        })
                    })
                    await updatePanelSetting(projectId, key, data)
                    toaster.positive('Panel setting saved', { autoHideDuration: 2000 })
                },
            })
        )
        return () => {
            subscription.unsubscribe()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId, store, key, eventBus])

    const Nodes = useMemo(() => {
        return tree.map((node, i) => (
            <WrapedWidgetNode key={node.id} id={node.id} type={node.type} path={[i]} childWidgets={node.children} />
        ))
    }, [tree])

    const form = new WidgetFormModel().initPanelSchema()

    // console.log('tree', api)

    return (
        <div>
            {Nodes}
            <WidgetFormModal
                form={form}
                id={editWidget?.payload?.id}
                isShow={isPanelModalOpen}
                setIsShow={setisPanelModalOpen}
                store={store}
                handleFormSubmit={({ formData }: any) => {
                    // @ts-ignore
                    actions[editWidget?.type]?.(formData)
                    setisPanelModalOpen(false)
                }}
            />
            <WidgetPreviewModal
                id={viewWidget?.payload?.id}
                isShow={isPanelPreviewModalOpen}
                setIsShow={setisPanelPreviewModalOpen}
                store={store}
            />
        </div>
    )
}

export default WidgetRenderTree

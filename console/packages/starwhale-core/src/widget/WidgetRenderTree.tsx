import React, { useContext, useEffect, useMemo, useState } from 'react'
import deepEqual from 'fast-deep-equal'
import { Subscription } from 'rxjs'
import { updatePanelSetting } from '@/domain/panel/services/panel'
import { useParams } from 'react-router'
import { toaster } from 'baseui/toast'
import { useJob } from '@/domain/job/hooks/useJob'
import { useEditorContext } from '../context/EditorContextProvider'
import withWidgetDynamicProps from './withWidgetDynamicProps'
import { WidgetRenderer } from './WidgetRenderer'
import WidgetFormModel from '../form/WidgetFormModel'
import { useFetchPanelSetting } from '@/domain/panel/hooks/useSettings'
import { WidgetProps } from '../types'
import { PanelAddEvent } from '../events'
import { BusEvent, BusEventType } from '../events/types'
import { PanelEditEvent, PanelSaveEvent, SectionAddEvent } from '../events/app'
import widget from '../../../starwhale-widgets/src/SectionWidget/index'
import { PANEL_DYNAMIC_MATCHES, replacer } from '../utils/replacer'
import _ from 'lodash'
import produce from 'immer'

export const WrapedWidgetNode = withWidgetDynamicProps(function WidgetNode(props: WidgetProps) {
    const { childWidgets, path } = props
    return (
        <WidgetRenderer {...props}>
            {childWidgets &&
                childWidgets.length > 0 &&
                childWidgets.map(({ children: childChildren, ...childRest }, i) => (
                    <WrapedWidgetNode
                        // @ts-ignore
                        key={[...path, 'children', i]}
                        path={[...path, 'children', i]}
                        childWidgets={childChildren}
                        {...childRest}
                    />
                ))}
        </WidgetRenderer>
    )
})

enum PanelEditAction {
    ADD = 'add-panel',
    EDIT = 'edit-panel',
}

export function WidgetRenderTree() {
    const { projectId, jobId } = useParams<{ projectId: string; jobId: string }>()
    const { job } = useJob()
    const { store, eventBus, dynamicVars } = useEditorContext()
    const api = store()
    const tree = store((state) => state.tree, deepEqual)
    // @ts-ignore
    const [editWidget, setEditWidget] = useState<BusEventType>(null)
    const [isPanelModalOpen, setisPanelModalOpen] = React.useState(false)
    // const key = job?.modelName ? `modelName-${job?.modelName}` : ''
    // const key = jobId ? `evaluation-${jobId}` : ''
    const key = 'evaluation'

    console.log('Tree', tree, job)

    // useBusEvent(eventBus, { type: 'add-panel' }, (evt) => {
    //     console.log(evt)
    // })

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

    const actions = {
        [PanelEditAction.ADD]: handleAddPanel,
        [PanelEditAction.EDIT]: handleEditPanel,
    }

    // use  api store
    // @FIXME refactor load/save, now only global inject what about table row inject ?
    const setting = useFetchPanelSetting(projectId, key)
    useEffect(() => {
        // @FIXME make sure dynamicVars to be exists!
        if (setting.data && dynamicVars?.prefix) {
            try {
                let data = JSON.parse(setting.data)
                for (let id in data?.widgets) {
                    _.set(data.widgets, id, replacer(PANEL_DYNAMIC_MATCHES).toOrigin(data.widgets[id], dynamicVars))
                }
                console.log('origin', data)
                if (store.getState().time < data?.time) store.setState(data)
            } catch (e) {
                console.log(e)
            }
        }
    }, [setting, dynamicVars?.prefix])

    // subscription
    useEffect(() => {
        const subscription = new Subscription()
        subscription.add(
            eventBus.getStream(PanelAddEvent).subscribe({
                next: (evt) => {
                    console.log(evt)
                    setisPanelModalOpen(true)
                    setEditWidget(evt)
                },
            })
        )
        subscription.add(
            eventBus.getStream(PanelEditEvent).subscribe({
                next: (evt) => {
                    console.log(evt)
                    setisPanelModalOpen(true)
                    setEditWidget(evt)
                },
            })
        )
        subscription.add(
            eventBus.getStream(SectionAddEvent).subscribe({
                next: (evt) => {
                    console.log(evt)
                    handleAddSection(evt.payload)
                },
            })
        )
        subscription.add(
            eventBus.getStream(PanelSaveEvent).subscribe({
                next: async (evt) => {
                    store.setState({
                        key,
                        time: Date.now(),
                    })
                    let data = store.getState()
                    if (key) {
                        for (let id in data?.widgets) {
                            data = produce(data, (temp) => {
                                _.set(temp.widgets, id, replacer(PANEL_DYNAMIC_MATCHES).toTemplate(temp.widgets[id]))
                            })
                        }
                        await updatePanelSetting(projectId, key, data)
                        toaster.positive('Panel setting saved', { autoHideDuration: 2000 })
                    }
                },
            })
        )
        return () => {
            subscription.unsubscribe()
        }
    }, [])

    const Nodes = useMemo(() => {
        return tree.map((node, i) => (
            <WrapedWidgetNode key={node.id} id={node.id} type={node.type} path={[i]} childWidgets={node.children} />
        ))
    }, [tree])

    return (
        <div>
            {Nodes}
            <WidgetFormModel
                id={editWidget?.payload?.id}
                isShow={isPanelModalOpen}
                setIsShow={setisPanelModalOpen}
                store={store}
                handleFormSubmit={({ formData }: any) => {
                    // @ts-ignore
                    actions[editWidget.type]?.(formData)
                    setisPanelModalOpen(false)
                }}
            />
        </div>
    )
}

export default WidgetRenderTree

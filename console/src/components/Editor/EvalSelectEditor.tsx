import React from 'react'
import { witEditorContext } from './Editor'
import { WidgetRenderTree, WidgetRenderTreePropsT } from '@starwhale/core/widget'
import { WidgetStateT, useEventCallback } from '@starwhale/core'

const initialState: WidgetStateT = {
    key: 'widgets',
    tree: [
        {
            type: 'ui:section',
            optionConfig: {
                isEvaluationList: true,
            },
        },
    ],
    widgets: {},
    defaults: {},
}

const Editor = witEditorContext<WidgetRenderTreePropsT>(WidgetRenderTree, initialState)

function EvalSelectEditor() {
    const onStateChange = useEventCallback((state) => {
        console.log('onStateChange', state)
    })

    return <Editor initialState={''} onStateChange={onStateChange} />
}

export { EvalSelectEditor }
export default EvalSelectEditor

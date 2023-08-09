import React from 'react'
import { witEditorContext } from './Editor'
import { WidgetRenderTree } from '@starwhale/core/widget'

const initialState = {
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

const Editor = witEditorContext(WidgetRenderTree, initialState)

function EvalSelectEditor() {
    return <Editor />
}

export { EvalSelectEditor }
export default EvalSelectEditor

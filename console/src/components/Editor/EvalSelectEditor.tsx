import React from 'react'
import { witEditorContext, initialState } from './Editor'
import { WidgetRenderTree } from '@starwhale/core/widget'

const Editor = witEditorContext(WidgetRenderTree, initialState)

function EvalSelectEditor() {
    return (
        <div>
            <Editor />
        </div>
    )
}

export { EvalSelectEditor }
export default EvalSelectEditor

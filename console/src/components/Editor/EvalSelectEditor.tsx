import { withEditorContext } from './Editor'
import { WidgetRenderTree, withReportWidgets } from '@starwhale/core/widget'
import { WidgetStateT } from '@starwhale/core'

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

const EvalSelectEditor = withReportWidgets(withEditorContext(WidgetRenderTree, initialState))

export { EvalSelectEditor }
export default EvalSelectEditor

import { withEditorContext } from './Editor'
import { WidgetRenderTree, WidgetRenderTreePropsT, withReportWidgets } from '@starwhale/core/widget'
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

const EvalSelectEditor = withReportWidgets(withEditorContext<WidgetRenderTreePropsT>(WidgetRenderTree, initialState))

export { EvalSelectEditor }
export default EvalSelectEditor

import { witEditorContext } from './Editor'
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

const EvalSelectEditor = witEditorContext<WidgetRenderTreePropsT>(withReportWidgets(WidgetRenderTree), initialState)

export { EvalSelectEditor }
export default EvalSelectEditor

import { RJSFSchema, UiSchema, WidgetProps, RegistryWidgetsType } from '@rjsf/utils'
import SelectWidget from '@starwhale/ui/form/widgets/SelectWidget'

const DataColumnSelectWidget = (props: WidgetProps) => {
    const { enumOptions, enumDisabled } = props.options

    return <SelectWidget {...props} />
}

import Select from '../Select'
import { KIND, Operators } from './constants'
import { FilterT } from './types'

// a = 1 : label + operator + field

function Filter(options: FilterT): FilterT {
    const operatorOptions = options.operators.map((key: string) => {
        const operator = Operators[key]
        return {
            id: operator.key,
            label: operator.label,
        }
    })

    return {
        kind: options.kind,
        operators: options.operators,
        renderOperator: function RenderOperator(props: any) {
            return <OperatorSelector {...props} options={operatorOptions} />
        },
        renderFieldValue: options?.renderFieldValue ?? <></>,
    }
}
export default Filter

function OperatorSelector(props: any) {
    const { value, onChange, options } = props
    return (
        <Select
            value={
                value
                    ? [
                          {
                              id: value,
                          },
                      ]
                    : []
            }
            onChange={(param: any) => onChange?.(param.option.id)}
            options={options}
            size='compact'
            clearable={false}
        />
    )
}

import React from 'react'
import { PopoverContainer, Label } from '../../Popover'
import FieldDefault from '../components/FieldDefault'
import { FilterT, FilterTypeOperators, KIND, Operators } from '../types'
import FieldDatetime from '../components/FieldDatetime'

const normalize = (v: string) => ['..', v.split('/').pop()].join('/')

const defaultRenderFieldLabel = (value: any) => (typeof value === 'string' ? normalize(value) : value)

function FilterBuilder(options: FilterT = {}): FilterT {
    return {
        kind: options.kind,
        fieldOptions: options.fieldOptions || [],
        operatorOptions: options.operatorOptions || [],
        valueOptions: options.valueOptions || [],
        renderField: function RenderField({
            options: renderOptions = [],
            optionFilter = () => true,
            isEditing = false,
            ...rest
        }) {
            return (
                <PopoverContainer
                    {...rest}
                    options={renderOptions.filter(optionFilter)}
                    isOpen={isEditing}
                    onItemSelect={({ item }) => rest.onChange?.(item.type)}
                >
                    {isEditing && rest.renderInput?.()}
                    {!isEditing && <Label {...rest}>{renderOptions.find((v) => v.id === rest.value)?.label}</Label>}
                </PopoverContainer>
            )
        },
        renderOperator: function RenderOperator({
            options: renderOptions = [],
            optionFilter = () => true,
            isEditing = false,
            ...rest
        }) {
            return (
                <PopoverContainer
                    {...rest}
                    options={renderOptions.filter(optionFilter)}
                    isOpen={isEditing}
                    onItemSelect={({ item }) => rest.onChange?.(item.type)}
                >
                    {isEditing && rest.renderInput?.()}
                    {!isEditing && (
                        <Label {...rest}>
                            {renderOptions.find((v) => v.id === rest.value)?.label} {rest.renderAfter?.()}
                        </Label>
                    )}
                </PopoverContainer>
            )
        },
        renderFieldValue: FieldDefault,
        renderValue: options?.renderValue ?? undefined,
        renderFieldLabel: options?.renderFieldLabel ?? defaultRenderFieldLabel,
        ...options,
    }
}

function getOperatorOpeionsByKind(kind: KIND) {
    return FilterTypeOperators[kind].map((v) => ({
        id: Operators[v].key,
        type: Operators[v].key,
        name: Operators[v].key,
        label: Operators[v].label,
    }))
}

function FilterDatetime(options: FilterT): FilterT {
    return FilterBuilder({
        ...options,
        kind: KIND.DATATIME,
        operatorOptions: getOperatorOpeionsByKind(KIND.DATATIME),
        renderFieldValue: FieldDatetime,
    })
}

function FilterBoolean(options: FilterT): FilterT {
    return FilterBuilder({
        ...options,
        kind: KIND.BOOLEAN,
        operatorOptions: getOperatorOpeionsByKind(KIND.BOOLEAN),
    })
}

function FilterNumberical(options: FilterT): FilterT {
    return FilterBuilder({
        ...options,
        kind: KIND.NUMERICAL,
        operatorOptions: getOperatorOpeionsByKind(KIND.NUMERICAL),
    })
}

function FilterString(options: FilterT): FilterT {
    return FilterBuilder({
        ...options,
        kind: KIND.STRING,
        operatorOptions: getOperatorOpeionsByKind(KIND.STRING),
    })
}

function FilterStrinWithContains(options: FilterT): FilterT {
    return FilterBuilder({
        ...options,
        kind: KIND.STRING,
        operatorOptions: getOperatorOpeionsByKind(KIND.STRING_WITH_CONTAINS),
    })
}

function FilterBuilderByColumnType(type: string, options: FilterT): FilterT {
    switch (type) {
        default:
        case 'STRING':
            return FilterString(options)
        case 'BOOL':
            return FilterBoolean(options)
        case 'INT8':
        case 'INT16':
        case 'INT32':
        case 'INT64':
        case 'FLOAT16':
        case 'FLOAT32':
        case 'FLOAT64':
            return FilterNumberical(options)
    }
}

export function createBuilder({
    key,
    fields = [],
    list = [],
}: {
    key: string
    fields: string[]
    list: any[]
}): (args: any) => FilterT {
    const valueHints = new Set()
    // collect value hints
    list.forEach((item) => {
        if (valueHints.size < 20) valueHints.add(item[key])
    })

    const fieldOptions = fields.map((field) => {
        return {
            id: field,
            type: field,
            label: field,
        }
    })

    const valueOptions = [...valueHints].map((v) => ({
        id: v,
        type: v,
        label: v,
    }))

    const build =
        (cached) =>
        (_FilterBuilder, _options = {}) =>
            _FilterBuilder({ ...cached, ..._options })

    return build({
        fieldOptions,
        valueOptions,
    })
}

export {
    FilterBuilder,
    FilterBoolean,
    FilterNumberical,
    FilterString,
    FilterDatetime,
    FilterBuilderByColumnType,
    FilterStrinWithContains,
}
export default FilterBuilder

import React from 'react'
import { FilterT } from '../types'
import { PopoverContainer, Label } from '../components/PopoverContainer'
import FieldDefault from '../components/FieldDefault'

const normalize = (v: string) => ['..', v.split('/').pop()].join('/')

function Filter(options: FilterT): FilterT {
    return {
        kind: options.kind,
        operators: options.operators,
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
                    {!isEditing && (
                        <Label {...rest}>{typeof rest.value === 'string' ? normalize(rest.value) : rest.value}</Label>
                    )}
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
        // renderFieldValue: options?.renderFieldValue ?? <></>,
    }
}
export default Filter

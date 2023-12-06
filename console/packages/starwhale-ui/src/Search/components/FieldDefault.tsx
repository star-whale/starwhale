import React from 'react'
import { Label, PopoverContainer, MultiSelectMenu, SingleSelectMenu } from '../../Popover'

function FieldDefault({ options: renderOptions = [], optionFilter = () => true, isEditing = false, ...rest }) {
    return (
        <PopoverContainer
            {...rest}
            options={renderOptions.filter(optionFilter)}
            // only option exsit will show popover
            isOpen={isEditing}
            Content={!rest.multi ? SingleSelectMenu : MultiSelectMenu}
            onItemSelect={({ item }) => rest.onChange?.(item.type)}
            onItemIdsConfirm={(ids = []) => rest.onChange?.(ids.join(','))}
            onItemIdsChange={(ids = []) => rest.onInputChange?.(ids.join(','))}
        >
            {isEditing && rest.renderInput?.()}
            {!isEditing && (
                <Label {...rest}>
                    {Array.isArray(rest.value) ? rest.value.join(',') : rest.value} {rest.renderAfter?.()}
                </Label>
            )}
        </PopoverContainer>
    )
}

export default FieldDefault

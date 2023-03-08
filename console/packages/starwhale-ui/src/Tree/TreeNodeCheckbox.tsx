import React from 'react'
import { TreeLabelInteractable } from 'baseui/tree-view'
import Checkbox from '../Checkbox'
import { TreeNodeContainer } from './StyledComponent'

export default function TreeNodeCheckbox({ label, value, onChange, actions }) {
    return (
        <TreeLabelInteractable>
            <TreeNodeContainer>
                <Checkbox checked={value} onChange={onChange}>
                    {label}
                </Checkbox>
                {actions}
            </TreeNodeContainer>
        </TreeLabelInteractable>
    )
}

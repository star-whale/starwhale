import React from 'react'
import Checkbox from '../Checkbox'
import { TreeNodeContainer } from './StyledComponent'
import { TreeNodePropsT } from './types'
import Radio from '../Radio'
import { TreeLabelInteractable } from '../base/tree-view'

export default function TreeNode({
    multiple,
    selectable,
    isSelected = false,
    isSelectedIndeterminate = false,
    label,
    onChange,
    actions,
    node,
}: TreeNodePropsT) {
    const $isStaticLabel = !selectable
    const $isRadioLeaf = selectable && !multiple && node.isLeafNode

    if ($isStaticLabel || $isRadioLeaf) {
        return (
            <TreeNodeContainer>
                {label}
                {actions}
            </TreeNodeContainer>
        )
    }

    const TreeNodeitem = () => {
        if (multiple) {
            return (
                <Checkbox
                    checked={isSelected}
                    isIndeterminate={isSelectedIndeterminate}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => onChange?.(e.target?.checked)}
                >
                    {label}
                </Checkbox>
            )
        }
        return (
            <Radio
                value={isSelected}
                overrides={{
                    Label: {
                        style: {
                            marginTop: '0px',
                            marginBottom: '0px',
                        },
                    },
                }}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => onChange?.(e.target?.value)}
            >
                {label}
            </Radio>
        )
    }

    return (
        <TreeLabelInteractable>
            <TreeNodeContainer>
                {TreeNodeitem()}
                {actions}
            </TreeNodeContainer>
        </TreeLabelInteractable>
    )
}

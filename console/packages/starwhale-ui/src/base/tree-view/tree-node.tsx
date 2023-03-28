/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/

import * as React from 'react'

import { StyledTreeItemList, StyledTreeItem } from './styled-components'
import type { TreeNodeProps } from './types'

import StyledTreeLabel from './tree-label'

import { getOverride, getOverrideProps } from '../helpers/overrides'

import type { SyntheticEvent } from 'react'

export default class TreeNode extends React.Component<TreeNodeProps> {
    treeItemRef = React.createRef<HTMLLIElement>()

    componentDidMount() {
        this.props.addRef(this.props.getId(this.props.node), this.treeItemRef)
    }

    componentWillUnmount() {
        this.props.removeRef(this.props.getId(this.props.node))
    }

    onToggle = () => {
        const { onToggle, node } = this.props
        if (onToggle) {
            onToggle(node)
        }
    }

    onFocus = (e: SyntheticEvent) => {
        if (e && e.target !== this.treeItemRef.current) return
        const { onFocus } = this.props
        if (onFocus) {
            onFocus(e)
        }
    }

    render() {
        const {
            indentGuides,
            node,
            getId,
            onToggle,
            overrides = {},
            renderAll,
            selectedNodeId,
            onKeyDown,
            onFocus,
            onBlur,
            addRef,
            removeRef,
            isFocusVisible,
        } = this.props
        const { children, isExpanded, label } = node
        const hasChildren = children && children.length !== 0
        const {
            TreeItemList: TreeItemListOverride,
            TreeItem: TreeItemOverride,
            TreeLabel: TreeLabelOverride,
        } = overrides
        const TreeItemList = getOverride(TreeItemListOverride) || StyledTreeItemList
        const TreeItem = getOverride(TreeItemOverride) || StyledTreeItem
        const TreeLabel = getOverride(TreeLabelOverride) || StyledTreeLabel
        return (
            <TreeItem
                role='treeitem'
                ref={this.treeItemRef}
                data-nodeid={getId(node)}
                tabIndex={selectedNodeId === getId(node) ? 0 : -1}
                onKeyDown={(e: KeyboardEvent) => onKeyDown && onKeyDown(e, node)}
                onBlur={onBlur}
                onFocus={this.onFocus}
                aria-expanded={isExpanded}
                $isLeafNode={!hasChildren}
                {...getOverrideProps(TreeItemOverride)}
            >
                <TreeLabel
                    onClick={this.onToggle}
                    node={node}
                    hasChildren={hasChildren}
                    isExpanded={isExpanded}
                    isSelected={selectedNodeId === getId(node)}
                    isFocusVisible={isFocusVisible}
                    label={label}
                    $isChildNode={true}
                    overrides={overrides}
                    {...getOverrideProps(TreeLabelOverride)}
                />
                {children && (isExpanded || renderAll) && (
                    <TreeItemList
                        role='group'
                        $indentGuides={!!indentGuides}
                        $isChildNode={true}
                        $expanded={!!isExpanded}
                        {...getOverrideProps(TreeItemListOverride)}
                    >
                        {children.map((node, index) => (
                            <TreeNode
                                indentGuides={!!indentGuides}
                                renderAll={renderAll}
                                key={index}
                                node={node}
                                getId={getId}
                                onToggle={onToggle}
                                overrides={overrides}
                                selectedNodeId={selectedNodeId}
                                onKeyDown={onKeyDown}
                                onFocus={onFocus}
                                onBlur={onBlur}
                                addRef={addRef}
                                removeRef={removeRef}
                                isFocusVisible={isFocusVisible}
                            />
                        ))}
                    </TreeItemList>
                )}
            </TreeItem>
        )
    }
}

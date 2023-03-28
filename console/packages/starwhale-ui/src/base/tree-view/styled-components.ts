/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/

import { styled } from 'baseui/styles'

export const StyledTreeItemList = styled<
    'ul',
    {
        $indentGuides?: boolean
        $isChildNode?: boolean
        $expanded?: boolean
    }
>('ul', ({ $theme, $indentGuides, $isChildNode, $expanded = true }) => {
    const direction: string = $theme.direction === 'rtl' ? 'Right' : 'Left'
    return {
        marginTop: 0,
        marginBottom: 0,
        [`margin${direction}`]: $isChildNode ? $theme.sizing.scale550 : 0,
        overflow: 'auto',
        paddingTop: 0,
        paddingBottom: 0,
        [`padding${direction}`]: $isChildNode ? $theme.sizing.scale200 : 0,
        position: 'relative',
        outline: 'none',
        display: $expanded ? 'block' : 'none',
        [`border${direction}`]: $indentGuides && $isChildNode ? `1px solid ${$theme.colors.borderOpaque}` : 'none',
    }
})

StyledTreeItemList.displayName = 'StyledTreeItemList'

export const StyledTreeItem = styled<
    'li',
    {
        $isLeafNode?: boolean
    }
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
>('li', ({ $theme, $isLeafNode }) => {
    const direction: string = $theme.direction === 'rtl' ? 'Right' : 'Left'

    return {
        cursor: $isLeafNode ? 'auto' : 'pointer',
        marginTop: 0,
        marginBottom: 0,
        marginLeft: 0,
        marginRight: 0,
        overflow: 'auto',
        paddingTop: 0,
        paddingBottom: 0,
        paddingLeft: 0,
        paddingRight: 0,
        position: 'relative',
        outline: 'none',
        [`padding${direction}`]: $theme.sizing.scale200,
    }
})

StyledTreeItem.displayName = 'StyledTreeItem'

export const StyledItemContent = styled<
    'div',
    {
        $isSelected: boolean
        $isFocusVisible: boolean
    }
>('div', ({ $theme, $isSelected, $isFocusVisible }) => {
    return {
        ...$theme.typography.font300,
        'alignItems': 'center',
        'boxSizing': 'border-box',
        'color': $theme.colors.primary,
        'display': 'flex',
        'marginTop': 0,
        'marginBottom': 0,
        'marginLeft': 0,
        'marginRight': 0,
        'paddingLeft': $theme.sizing.scale200,
        'paddingRight': $theme.sizing.scale200,
        'paddingTop': $theme.sizing.scale100,
        'paddingBottom': $theme.sizing.scale100,
        'outline': $isSelected && $isFocusVisible ? `3px solid ${$theme.colors.accent}` : 'none',
        'outlineOffset': '-3px',

        ':hover': {
            // @ts-ignore
            backgroundColor: $theme.brandBgSecondary,
        },
    }
})

StyledItemContent.displayName = 'StyledItemContent'

export const StyledIconContainer = styled('div', ({ $theme }) => {
    const marginDirection: string = $theme.direction === 'rtl' ? 'marginLeft' : 'marginRight'
    return {
        alignItems: 'center',
        display: 'flex',
        justifyContent: 'center',
        marginTop: 0,
        marginBottom: 0,
        [marginDirection]: $theme.sizing.scale200,
    }
})

StyledIconContainer.displayName = 'StyledIconContainer'

export const StyledNoIconContainer = styled('div', ({ $theme }) => {
    const marginDirection: string = $theme.direction === 'rtl' ? 'marginLeft' : 'marginRight'
    return {
        alignItems: 'center',
        display: 'flex',
        justifyContent: 'center',
        marginTop: 0,
        marginBottom: 0,
        [marginDirection]: $theme.sizing.scale200,
        width: 0,
        height: '1em',
    }
})

StyledNoIconContainer.displayName = 'StyledNoIconContainer'

export const StyledLabelInteractable = styled('div', () => ({
    width: '100%',
}))
StyledLabelInteractable.displayName = 'StyledLabelInteractable'

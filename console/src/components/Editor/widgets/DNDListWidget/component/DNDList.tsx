/* eslint-disable */
/* @ts-nocheck */

import * as React from 'react'
import type { ListProps, SharedStylePropsArgT } from 'baseui/dnd-list'
import Accordion from '@/components/Accordion'
import { Panel, StatefulPanel, StatelessAccordion } from 'baseui/accordion'
import { StyledListItem } from 'baseui/menu'
import IconFont from '@/components/IconFont'
import { createUseStyles } from 'react-jss'
import { expandPadding } from '@/utils'
import { StyledLabel } from 'baseui/checkbox'
import { useEditorContext } from '@/components/Editor/context/EditorContextProvider'
import { get, set } from 'lodash'
import WidgetPlugin from '@/components/Editor/Widget/WidgetPlugin'
import useSelector, { getTree } from '../../../hooks/useSelector'
import { List, arrayMove, arrayRemove, StyledItem, StyledDragHandle } from './index'

const useStyles = createUseStyles({
    dragHandle: {},
})

export function PanelWrapper({ children, $isDragged, ...rest }) {
    const [expanded, setExpanded] = React.useState<React.Key[]>(['P1'])

    React.useEffect(() => {
        setExpanded($isDragged ? [] : ['p1'])
    }, [$isDragged])

    console.log('PanelWrapper', $isDragged, rest, expanded)

    return (
        <StatelessAccordion
            expanded={expanded}
            onChange={({ key, expanded }) => {
                setExpanded(expanded)
            }}
        >
            <Panel key='P1' title='Panel 1' renderAll={false} expanded={false}>
                {children}
            </Panel>
        </StatelessAccordion>
    )
}

export function DragHandle({ children, ...rest }: SharedStylePropsArgT & { children: React.ReactNode }) {
    // console.log(rest)
    return (
        <StyledDragHandle
            {...rest}
            style={{
                position: 'absolute',
                margin: 'auto',
                top: '14px',
                left: 0,
                right: 0,
                zIndex: '10',
            }}
        >
            <IconFont type='drag' size={20} />
        </StyledDragHandle>
    )
}

export function Label({ children, ...rest }: SharedStylePropsArgT & { children: React.ReactNode }) {
    // console.log('label', rest)
    return (
        <StyledLabel {...rest} style={{ flex: '1' }}>
            {rest.$value}
        </StyledLabel>
    )
}
export const Item = React.forwardRef(({ style, ...rest }: SharedStylePropsArgT, ref) => {
    // console.log(rest)
    return (
        <StyledItem
            ref={ref}
            {...rest}
            style={{ ...expandPadding('0', '0', '0', '0'), width: '100%', position: 'relative', ...style }}
        />
    )
})

export default function DNDList(props: any) {
    return (
        <List
            // @ts-ignore
            overrides={
                {
                    DragHandle,
                    // Item,
                    Label,
                } as any
            }
            items={props.children}
            onChange={({ oldIndex, newIndex }) => {
                props.onChange?.(oldIndex, newIndex)
            }}
        />
    )
}

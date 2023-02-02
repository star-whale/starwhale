import React from 'react'
import { mergeOverrides } from '../utils'
import { Tabs as BaseTabs, TabsProps } from 'baseui/tabs'

export interface ITabsProps extends TabsProps {
    overrides?: TabsProps['overrides']
}

export function SpaceTabs({ ...props }: ITabsProps) {
    const overrides = mergeOverrides(
        {
            TabBar: {
                style: {
                    display: 'flex',
                    gap: '0',
                    paddingLeft: 0,
                    paddingRight: 0,
                    borderRadius: '4px',
                    backgroundColor: '#fff',
                },
            },
            Tab: {
                style: ({ $active }) => ({
                    'flex': 1,
                    'textAlign': 'center',
                    'color': $active ? ' #2B65D9' : 'rgba(2,16,43,0.60)',
                    'marginLeft': '0',
                    'paddingTop': '6px',
                    'paddingBottom': '9px',
                    'height': '32px',
                    'width': '40px',
                    'border': $active ? '1px solid #2B65D9' : '1px solid #CFD7E6',
                    'zIndex': $active ? '1' : '0',
                    'boxSizing': 'border-box',
                    'marginRight': '-1px',
                    'borderInlineStartWidth': '0',
                    ':hover': {
                        color: '#5181E0',
                    },
                    ':last-child': {
                        borderInlineStartWidth: $active ? '1px' : '0',
                        borderTopRightRadius: '4px',
                        borderBottomRightRadius: '4px',
                    },
                    ':not(:first-child, :last-child)': {
                        borderInlineStartWidth: $active ? '1px' : '0',
                    },
                    ':first-child': {
                        borderInlineStartWidth: '1px',
                        borderTopLeftRadius: '4px',
                        borderBottomLeftRadius: '4px',
                    },
                }),
            },
        },
        props.overrides
    )

    // eslint-disable-next-line  react/jsx-props-no-spreading
    return (
        <BaseTabs {...props} overrides={overrides}>
            {props.children}
        </BaseTabs>
    )
}

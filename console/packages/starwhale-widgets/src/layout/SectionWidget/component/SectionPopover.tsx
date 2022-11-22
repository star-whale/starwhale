import React from 'react'
import { StatefulPopover, PLACEMENT } from 'baseui/popover'
import { StatefulMenu } from 'baseui/menu'
import IconFont from '@/components/IconFont'

const COLUMN_OPTIONS = [
    { label: 'Rename', type: 'rename' },
    { label: 'Add above', type: 'addAbove' },
    { label: 'Add belove', type: 'addBelow' },
    { label: 'Delete', type: 'delete' },
]

export default function SectionPopover({ onOptionSelect }) {
    return (
        <StatefulPopover
            focusLock
            placement={PLACEMENT.bottom}
            content={({ close }) => (
                <StatefulMenu
                    items={COLUMN_OPTIONS}
                    onItemSelect={({ item }) => {
                        onOptionSelect(item)
                        close()
                    }}
                    overrides={{
                        List: { style: { height: '160px', width: '150px' } },
                        Option: {
                            props: {
                                getItemLabel: (item: { label: string; type: string }) => {
                                    const icon = {
                                        rename: <IconFont type='edit' />,
                                        addAbove: <IconFont type='a-Addabove' />,
                                        addBelow: <IconFont type='a-Addbelow' />,
                                        delete: <IconFont type='delete' />,
                                    }

                                    return (
                                        <div style={{ display: 'flex', justifyContent: 'flex-start', gap: '13px' }}>
                                            {icon?.[item.type as keyof typeof icon]}
                                            {item.label}
                                        </div>
                                    )
                                },
                            },
                        },
                    }}
                />
            )}
        >
            <div
                style={{
                    alignItems: 'center',
                    marginLeft: 'auto',
                    right: 0,
                    top: -6,
                    display: 'flex',
                    backgroundColor: '#F4F5F7',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    padding: '12px 9px',
                    height: '32px',
                }}
            >
                <IconFont
                    type='more'
                    size={14}
                    // style={{
                    //     display: props.isHovered && !props.compareable ? 'block' : 'none',
                    // }}
                />
            </div>
        </StatefulPopover>
    )
}

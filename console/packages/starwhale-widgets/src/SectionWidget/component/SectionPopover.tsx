import React from 'react'
import { StatefulPopover, PLACEMENT } from 'baseui/popover'
import { StatefulMenu } from 'baseui/menu'
import IconFont from '@starwhale/ui/IconFont'
// @FIXME move to ui
import { ConfirmButton } from '@/components/Modal/confirm'

const COLUMN_OPTIONS = [
    { label: 'Rename', type: 'rename' },
    { label: 'Add above', type: 'addAbove' },
    { label: 'Add belove', type: 'addBelow' },
    { label: 'Delete', type: 'delete' },
]

// @ts-ignore
export default function SectionPopover({ onOptionSelect }) {
    return (
        <StatefulPopover
            focusLock
            placement={PLACEMENT.bottom}
            content={({ close }) => (
                <StatefulMenu
                    items={COLUMN_OPTIONS}
                    onItemSelect={({ item }) => {
                        if (item.type === 'delete') {
                            close()
                            return
                        }
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
                                        delete: <IconFont type='delete' style={{ color: '#CC3D3D' }} />,
                                    }
                                    const menu = (
                                        <div
                                            style={{
                                                display: 'flex',
                                                justifyContent: 'flex-start',
                                                gap: '13px',
                                                color: item.type === 'delete' ? '#CC3D3D' : undefined,
                                                alignItems: 'center',
                                            }}
                                        >
                                            {icon?.[item.type as keyof typeof icon]}
                                            {item.label}
                                        </div>
                                    )

                                    if (item.type === 'delete') {
                                        return (
                                            <ConfirmButton
                                                as='link'
                                                title='Are you sure to delete this panel?'
                                                onClick={async (e) => {
                                                    e.preventDefault()
                                                    onOptionSelect(item)
                                                    close()
                                                }}
                                                overrides={{
                                                    BaseButton: {
                                                        style: {
                                                            width: 'calc(100% + 32px)',
                                                            textAlign: 'left',
                                                            justifyContent: 'flex-start',
                                                            margin: '-8px -16px',
                                                            padding: '8px 16px',
                                                            height: '36px',
                                                        },
                                                    },
                                                }}
                                            >
                                                {menu}
                                            </ConfirmButton>
                                        )
                                    }

                                    return menu
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
                <IconFont type='more' size={14} />
            </div>
        </StatefulPopover>
    )
}

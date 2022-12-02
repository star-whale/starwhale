import React from 'react'
import { StatefulPopover, PLACEMENT } from 'baseui/popover'
import { StatefulMenu } from 'baseui/menu'
import IconFont from '@starwhale/ui/IconFont'
// @FIXME move to ui
import { ConfirmButton } from '@/components/Modal/confirm'
import { expandMargin } from '@starwhale/ui/utils'
import { expandPadding } from '../../../../../src/utils/index'

const COLUMN_OPTIONS = [
    { label: 'Edit', type: 'edit' },
    { label: 'Remove', type: 'delete' },
]

// @ts-ignore
export default function ChartConfigPopover({ onOptionSelect }) {
    return (
        <StatefulPopover
            focusLock
            placement={PLACEMENT.bottom}
            content={({ close }) => (
                <StatefulMenu
                    items={COLUMN_OPTIONS}
                    onItemSelect={({ item }) => {
                        if (item.type === 'delete') return
                        onOptionSelect(item)
                        close()
                    }}
                    overrides={{
                        List: { style: { height: 'auto', width: '150px' } },
                        Option: {
                            props: {
                                getItemLabel: (item: { label: string; type: string }) => {
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
                                            {item.label}
                                        </div>
                                    )
                                    if (item.type === 'delete') {
                                        return (
                                            <ConfirmButton
                                                as='link'
                                                title={'Are you sure to delete this chart?'}
                                                onClick={async (e) => {
                                                    e.preventDefault
                                                    onOptionSelect(item)
                                                    close()
                                                }}
                                                overrides={{
                                                    BaseButton: {
                                                        style: {
                                                            width: 'calc(100% + 32px)',
                                                            textAlign: 'left',
                                                            justifyContent: 'flex-start',
                                                            ...expandMargin('-8px', '-16px', '-8px', '-16px'),
                                                            ...expandPadding('8px', '16px', '8px', '16px'),
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
                    // right: 0,
                    // top: -6,
                    display: 'flex',
                    backgroundColor: '#F4F5F7',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    padding: '4px',
                    height: '20px',
                }}
            >
                <IconFont type='setting' size={12} />
            </div>
        </StatefulPopover>
    )
}

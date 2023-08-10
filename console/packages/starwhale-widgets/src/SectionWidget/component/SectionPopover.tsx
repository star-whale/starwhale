import React from 'react'
import { StatefulPopover, PLACEMENT } from 'baseui/popover'
import { StatefulMenu } from 'baseui/menu'
import IconFont from '@starwhale/ui/IconFont'
import { expandMargin, expandPadding } from '@starwhale/ui/utils'
import { ConfirmButton } from '@starwhale/ui/Modal'
import useTranslation from '@/hooks/useTranslation'

// @ts-ignore
export default function SectionPopover({
    onOptionSelect,
    actions,
}: {
    onOptionSelect: (item: any) => void
    actions: any
}) {
    const [t] = useTranslation()

    const $options = React.useMemo(() => {
        return [
            actions.rename && { label: t('panel.rename'), type: 'rename' },
            actions.addAbove && { label: t('panel.add.above'), type: 'addAbove' },
            actions.addBelow && { label: t('panel.add.below'), type: 'addBelow' },
            actions.delete && { label: t('panel.delete'), type: 'delete' },
        ].filter(Boolean)
    }, [actions, t])

    return (
        <StatefulPopover
            focusLock
            placement={PLACEMENT.bottom}
            content={({ close }) => (
                <StatefulMenu
                    items={$options}
                    onItemSelect={({ item }) => {
                        if (item.type === 'delete') {
                            close()
                            return
                        }
                        onOptionSelect(item)
                        close()
                    }}
                    overrides={{
                        List: { style: { minHeight: '160px', minWidth: '150px' } },
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
                                                            height: '36px',
                                                            ...expandPadding('8px', '16px', '8px', '16px'),
                                                            ...expandMargin('-8px', '-16px', '-8px', '-16px'),
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

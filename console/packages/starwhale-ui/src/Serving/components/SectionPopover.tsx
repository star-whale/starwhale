import React from 'react'
import { StatefulPopover, PLACEMENT } from 'baseui/popover'
import { StatefulMenu } from 'baseui/menu'
import IconFont from '@starwhale/ui/IconFont'
import { expandMargin, expandPadding } from '@starwhale/ui/utils'
import { ConfirmButton } from '@starwhale/ui/Modal'
import useTranslation from '@/hooks/useTranslation'
import { ExtendButton } from '@starwhale/ui/Button'

export default function SectionPopover({ onOptionSelect }: { onOptionSelect: (item: any) => void }) {
    const [t] = useTranslation()

    const $options = React.useMemo(() => {
        return [
            { label: t('View Tasks'), type: 'viewlog' },
            { label: t('ft.online_eval.parameter.setting'), type: 'parameter' },
            // { label: t('panel.chart.reload'), type: 'reload' },
            { label: t('Cancel'), type: 'delete' },
        ].filter(Boolean)
    }, [t])

    return (
        <StatefulPopover
            focusLock
            triggerType='hover'
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
                                    const icon = {}
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
                                                title={t('Cancel.Confirm')}
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
            <ExtendButton kind='secondary' styleas={['menuoption']}>
                <IconFont type='more' size={14} />
            </ExtendButton>
        </StatefulPopover>
    )
}

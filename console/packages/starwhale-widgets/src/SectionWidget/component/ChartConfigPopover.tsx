import React from 'react'
import { StatefulPopover, PLACEMENT } from 'baseui/popover'
import { StatefulMenu } from 'baseui/menu'
import IconFont from '@starwhale/ui/IconFont'
import { ConfirmButton } from '@starwhale/ui/Modal'
import { expandMargin, expandPadding } from '@starwhale/ui/utils'
import useTranslation from '@/hooks/useTranslation'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { Button } from '@starwhale/ui/Button'
import { createUseStyles } from 'react-jss'

const useStyles = createUseStyles({
    icon: {
        alignItems: 'center',
        marginLeft: 'auto',
        display: 'flex',
        backgroundColor: '#F4F5F7',
        borderRadius: '4px',
        cursor: 'pointer',
        padding: '4px',
        height: '20px',
    },
})

// @ts-ignore
export default function ChartConfigPopover({ onOptionSelect }) {
    const styles = useStyles()
    const [t] = useTranslation()
    const [css] = themedUseStyletron()
    const $options = React.useMemo(() => {
        return [
            { label: t('panel.chart.edit'), type: 'edit' },
            { label: t('panel.chart.delete'), type: 'delete' },
        ]
    }, [t])

    return (
        <StatefulPopover
            // dismissOnClickOutside
            dismissOnEsc
            placement={PLACEMENT.bottom}
            content={({ close }) => (
                <StatefulMenu
                    items={$options}
                    onItemSelect={({ item }) => {
                        close()
                        if (item.type === 'delete') {
                            return
                        }
                        onOptionSelect(item)
                    }}
                    overrides={{
                        List: { style: { height: 'auto', width: '150px' } },
                        Option: {
                            props: {
                                getItemLabel: (item: { label: string; type: string }) => {
                                    const menu = (
                                        <div
                                            className={css({
                                                display: 'flex',
                                                justifyContent: 'flex-start',
                                                gap: '13px',
                                                color: item.type === 'delete' ? '#CC3D3D' : undefined,
                                                alignItems: 'center',
                                            })}
                                        >
                                            {item.label}
                                        </div>
                                    )
                                    if (item.type === 'delete') {
                                        return (
                                            <ConfirmButton
                                                as='link'
                                                title={t('panel.chart.delete.title')}
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
            <Button
                kind='tertiary'
                className={styles.icon}
                overrides={{
                    BaseButton: {
                        style: {
                            'backgroundColor': '#F4F5F7',
                            'color': 'rgba(2,16,43,0.60)',
                            ':hover': {
                                color: '#2B65D9;',
                            },
                            ':focus': {
                                color: '#2B65D9;',
                            },
                            ':active': {
                                color: '#2B65D9;',
                            },
                        },
                    },
                }}
            >
                <IconFont type='setting' size={12} />
            </Button>
        </StatefulPopover>
    )
}

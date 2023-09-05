import React from 'react'
import { createUseStyles } from 'react-jss'
import IconFont from '@starwhale/ui/IconFont'
import classnames from 'classnames'
import ChartConfigPopover from './ChartConfigPopover'
import { ExtendButton } from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'

const useStyles = createUseStyles({
    chartGroup: {
        position: 'absolute',
        right: '20px',
        top: '16px',
        display: 'flex',
        gap: '6px',
        zIndex: 2,
    },
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

export default function ChartConfigGroup({
    onEdit = () => {},
    onDelete = () => {},
    onPreview = () => {},
    onDownload = () => {},
    onReload = () => {},
}: {
    onEdit?: () => void
    onDelete?: () => void
    onPreview?: () => void
    onDownload?: () => void
    onReload?: () => void
}) {
    const styles = useStyles()
    const actions = {
        edit: onEdit,
        delete: onDelete,
    }
    const [t] = useTranslation()

    return (
        <div className={classnames('panel-operator', styles.chartGroup)}>
            <ChartConfigPopover
                onOptionSelect={(item: any) => {
                    // @ts-ignore
                    actions?.[item?.type]()
                }}
            />
            <ExtendButton
                kind='tertiary'
                className={styles.icon}
                onClick={() => onReload()}
                tooltip={t('panel.chart.reload')}
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
                <IconFont type='reset' size={12} />
            </ExtendButton>
            <ExtendButton
                kind='tertiary'
                className={styles.icon}
                onClick={() => onDownload()}
                tooltip={t('panel.chart.download')}
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
                <IconFont type='download' size={12} />
            </ExtendButton>
            <ExtendButton
                kind='tertiary'
                className={styles.icon}
                onClick={() => onPreview()}
                tooltip={t('panel.chart.preview')}
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
                <IconFont type='fullscreen' size={12} />
            </ExtendButton>
        </div>
    )
}

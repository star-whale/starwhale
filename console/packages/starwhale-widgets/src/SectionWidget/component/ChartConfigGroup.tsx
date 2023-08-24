import React from 'react'
import { createUseStyles } from 'react-jss'
import IconFont from '@starwhale/ui/IconFont'
import classnames from 'classnames'
import ChartConfigPopover from './ChartConfigPopover'
import Button from '@starwhale/ui/Button'

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

    return (
        <div className={classnames('panel-operator', styles.chartGroup)}>
            <ChartConfigPopover
                onOptionSelect={(item: any) => {
                    // @ts-ignore
                    actions?.[item?.type]()
                }}
            />
            <Button
                kind='tertiary'
                className={styles.icon}
                onClick={() => onReload()}
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
            </Button>
            <Button
                kind='tertiary'
                className={styles.icon}
                onClick={() => onDownload()}
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
            </Button>
            <Button
                kind='tertiary'
                className={styles.icon}
                onClick={() => onPreview()}
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
            </Button>
        </div>
    )
}

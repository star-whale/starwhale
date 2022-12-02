import React from 'react'
import ChartConfigPopover from './ChartConfigPopover'
import { createUseStyles } from 'react-jss'
import IconFont from '@starwhale/ui/IconFont'
import Button from '@starwhale/ui/Button'

const useStyles = createUseStyles({
    chartGroup: {
        position: 'absolute',
        right: '20px',
        top: '16px',
        display: 'flex',
        gap: '6px',
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
}: {
    onEdit?: () => void
    onDelete?: () => void
    onPreview?: () => void
}) {
    const styles = useStyles()
    const actions = {
        edit: onEdit,
        delete: onDelete,
    }

    return (
        <div className={styles.chartGroup}>
            <ChartConfigPopover
                onOptionSelect={(item: any) => {
                    // @ts-ignore
                    actions?.[item?.type]()
                }}
            />
            <div className={styles.icon} role='button' onClick={() => onPreview()} tabIndex={0}>
                <IconFont type='fullscreen' size={12} />
            </div>
        </div>
    )
}

import React from 'react'
import { createUseStyles } from 'react-jss'
import IconFont from '@starwhale/ui/IconFont'
import classnames from 'classnames'
import ChartConfigPopover from './ChartConfigPopover'

const useStyles = createUseStyles({
    chartGroup: {
        position: 'absolute',
        right: '20px',
        top: '16px',
        display: 'flex',
        gap: '6px',
        zIndex: 10,
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
        <div className={classnames('panel-operator', styles.chartGroup)}>
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

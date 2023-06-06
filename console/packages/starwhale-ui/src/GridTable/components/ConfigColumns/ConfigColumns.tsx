import React, { useRef, useEffect, useImperativeHandle, useState } from 'react'
import { useStyletron } from 'baseui'
import { Drawer } from 'baseui/drawer'
import { expandBorderRadius } from '@/utils'
import type { ColumnT, ConfigT } from '../../../base/data-table/types'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { Transfer } from '@starwhale/ui/Transfer'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'

const useStyles = createUseStyles({
    transferWrapper: {
        height: '100%',
    },
    transfer: {
        'height': '100%',
        '& .header': {
            display: 'flex',
            flexDirection: 'column',
            height: '56px',
            lineHeight: '56px',
        },
        '& .header--inline': {
            paddingLeft: 0,
            fontWeight: 'bold',
            color: '#02102B',
        },
        '& .header--drawer': {
            borderBottom: '1px solid #EEF1F6',
            paddingLeft: '20px',
            marginBottom: '20px',
            color: '#02102B',
        },
        '& .body': {
            display: 'flex',
            flexDirection: 'column',
            gap: '20px',
            height: 'calc(100% - 76px)',
            paddingLeft: '20px',
            paddingRight: '20px',
        },
        '& .body--inline': {
            paddingLeft: 0,
            paddingRight: 0,
            display: 'flex',
            flexDirection: 'column',
            gap: '20px',
            height: 'calc(100% - 76px)',
        },
    },
})

type PropsT = {
    isInline?: boolean
    view: ConfigT
    columns?: ColumnT[]
    onColumnsChange?: (columnSortedIds: T[], columnVisibleIds: T[], pinnedIds: T[]) => void
}

type T = string
const ConfigColumns = React.forwardRef<{ getConfig: () => any }, PropsT>((props, configRef) => {
    const styles = useStyles()
    const [, theme] = useStyletron()
    const [t] = useTranslation()
    const [isOpen, setIsOpen] = React.useState(false)
    const ref = useRef(null)
    const { columns } = props

    const Wrapper = React.useCallback(
        ({ children }) => {
            return props.isInline ? (
                <div className={`${styles.transfer} inherit-height`}>{children}</div>
            ) : (
                <Drawer
                    size={`${314 * 2 + 52 + 20 * 2}px`}
                    isOpen={isOpen}
                    autoFocus
                    onClose={() => setIsOpen(false)}
                    mountNode={ref.current as any}
                    showBackdrop={false}
                    animate={false}
                    overrides={{
                        Root: {
                            style: {
                                zIndex: '102',
                                margin: 0,
                                paddingBottom: '4px',
                            },
                        },
                        DrawerContainer: {
                            style: {
                                boxSizing: 'border-box',
                                padding: '0px 0 10px',
                                boxShadow: '0 4px 14px 0 rgba(0, 0, 0, 0.3)',
                                margin: 0,
                                ...expandBorderRadius('0'),
                            },
                        },
                        DrawerBody: {
                            style: {
                                marginLeft: 0,
                                marginRight: 0,
                                marginTop: 0,
                                marginBottom: 0,
                            },
                            props: {
                                className: styles.transfer,
                            },
                        },
                    }}
                >
                    {children}
                </Drawer>
            )
        },
        [props.isInline, isOpen, styles]
    )

    const [value, setValue] = useState<any>(props.view)

    useImperativeHandle(
        configRef,
        () => ({
            getConfig: () => {
                return value
            },
        }),
        [value]
    )

    useEffect(() => {
        setValue(props.view)
    }, [props.view])

    return (
        <div ref={ref} className={cn(styles.transferWrapper, 'inherit-height')}>
            {!props.isInline && (
                <Button
                    onClick={() => setIsOpen(!isOpen)}
                    icon='setting'
                    kind='tertiary'
                    overrides={{
                        BaseButton: {
                            style: {
                                height: '32px',
                                marginLeft: theme.sizing.scale500,
                            },
                        },
                    }}
                />
            )}
            <Wrapper>
                <div className={cn('header', props.isInline ? 'header--inline' : 'header--drawer')}>
                    {t('table.column.manage')}
                </div>
                <div className={cn('body', props.isInline ? 'body--inline' : '', 'inherit-height')}>
                    <Transfer
                        columns={columns}
                        isDragable
                        isSearchable
                        value={value}
                        onChange={(v) => {
                            setValue(v)
                            props.onColumnsChange?.(v.selectedIds, v.pinnedIds, v.ids)
                        }}
                    />
                </div>
            </Wrapper>
        </div>
    )
})

ConfigColumns.defaultProps = {
    isInline: false,
    columns: [],
    onColumnsChange: () => {},
}
export { ConfigColumns }

export default ConfigColumns

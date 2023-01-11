import React, { useMemo, useCallback, useRef, useEffect, useImperativeHandle, useContext, useState } from 'react'
import { SHAPE, SIZE, KIND } from 'baseui/button'
import { Search } from 'baseui/icon'
import { useStyletron } from 'baseui'
import { useHover } from 'react-use'
import { Drawer } from 'baseui/drawer'
import { LabelSmall } from 'baseui/typography'
import Button from '@/components/Button'
import Input from '@/components/Input'
import useSelection from '@starwhale/ui/utils/useSelection'
import { useDrawer } from '@/hooks/useDrawer'
import IconFont from '@/components/IconFont'
import { expandBorderRadius } from '@/utils'
import { matchesQuery } from './text-search'
import type { ColumnT, ConfigT } from './types'
import Checkbox from '../../Checkbox'
import { DnDContainer } from '../../DnD'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { useDeepEffect } from '@starwhale/core/utils'
import TransferList from '@starwhale/ui/Transfer/TransferList'
import { Transfer } from '@starwhale/ui/Transfer'

const useStyles = createUseStyles({
    transfer: {
        '& .header': {
            display: 'flex',
            flexDirection: 'column',
            height: '56px',
            lineHeight: '56px',
        },
        '& .header--inline': {
            borderTop: '1px solid #EEF1F6',
            paddingLeft: 0,
            marginTop: '20px',
            fontWeight: 'bold',
        },
        '& .header--drawer': {
            borderBottom: '1px solid #EEF1F6',
            paddingLeft: '20px',
            marginBottom: '20px',
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
        },
    },
})

type PropsT = {
    isInline?: boolean
    view: ConfigT
    columns: ColumnT[]
    onApply?: (columnSortedIds: T[], columnVisibleIds: T[], pinnedIds: T[]) => void
    onSave?: (view: ConfigT) => void
    onSaveAs?: (view: ConfigT) => void
}

type T = string
const ConfigManageColumns = React.forwardRef<{ getConfig: () => any }, PropsT>((props, configRef) => {
    const styles = useStyles()
    const [css, theme] = useStyletron()
    const [isOpen, setIsOpen] = React.useState(true)
    const { expandedWidth, expanded, setExpanded } = useDrawer()

    useEffect(() => {
        if (props.isInline) {
            return
        }
        if (isOpen && !expanded) {
            setExpanded(true)
        } else if (!isOpen && expanded) {
            setExpanded(false)
        }
    }, [props.isInline, isOpen, expanded, setExpanded, expandedWidth])

    const ref = useRef(null)
    const { columns } = props

    // useDeepEffect(() => {
    //     console.log('onApply', selectedIds, pinnedIds, sortedIds)
    //     props.onApply?.(selectedIds, pinnedIds, sortedIds)
    //     // eslint-disable-next-line react-hooks/exhaustive-deps
    // }, [selectedIds, sortedIds, pinnedIds])

    // useImperativeHandle(
    //     configRef,
    //     () => ({
    //         getConfig: () => {
    //             return {
    //                 selectedIds,
    //                 sortedIds,
    //                 pinnedIds,
    //             }
    //         },
    //     }),
    //     [selectedIds, sortedIds, pinnedIds]
    // )

    const Wrapper = React.useCallback(
        // eslint-disable-next-line react/no-unused-prop-types
        ({ children }: { children: React.ReactNode }) => {
            return props.isInline ? (
                <div className={styles.transfer}>{children}</div>
            ) : (
                <Drawer
                    size={`${314 * 2 + 52 + 20 * 2}px`}
                    isOpen={isOpen}
                    autoFocus
                    onClose={() => setIsOpen(false)}
                    overrides={{
                        Root: {
                            style: {
                                zIndex: '102',
                                margin: 0,
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
        [props.isInline, isOpen]
    )

    const [value, setValue] = useState(() => props.view)

    return (
        <div ref={ref}>
            {!props.isInline && (
                <Button
                    onClick={() => setIsOpen(!isOpen)}
                    shape={SHAPE.pill}
                    size={SIZE.compact}
                    as='withIcon'
                    startEnhancer={() => (
                        <IconFont
                            type='setting'
                            style={{
                                marginRight: '-5px',
                                marginTop: 'px',
                            }}
                        />
                    )}
                    overrides={{
                        BaseButton: {
                            style: {
                                height: '32px',
                                marginLeft: theme.sizing.scale500,
                            },
                        },
                    }}
                >
                    Manage Columns
                </Button>
            )}
            <Wrapper>
                <div className={cn('header', props.isInline ? 'header--inline' : 'header--drawer')}>Manage Columns</div>
                <div className={cn('body', props.isInline ? 'body--inline' : '')}>
                    <Transfer columns={columns} isDragable isSearchable value={value} onChange={setValue} />
                </div>
            </Wrapper>
        </div>
    )
})

ConfigManageColumns.defaultProps = {
    isInline: false,
    onApply: () => {},
    onSave: () => {},
    onSaveAs: () => {},
}
export default ConfigManageColumns

/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/

import React, { useMemo, useCallback, useRef } from 'react'
import { SHAPE, SIZE, KIND } from 'baseui/button'
import { Search, Icon } from 'baseui/icon'
import { useStyletron } from 'baseui'
import { useHover } from 'react-use'
import { Drawer } from 'baseui/drawer'
import { Checkbox } from 'baseui/checkbox'
import { LabelSmall } from 'baseui/typography'
import Button from '@/components/Button'
import Input from '@/components/Input'
import useSelection from '@/hooks/useSelection'
import { AiOutlinePushpin } from 'react-icons/ai'
import { RiDeleteBin6Line } from 'react-icons/ri'
import { DnDContainer } from '../DnD/DnDContainer'
import { matchesQuery } from './text-search'
import type { ColumnT, ConfigT } from './types'

type PropsT = {
    config: ConfigT
    columns: ColumnT[]
    onColumnSave?: (columnSortedIds: T[], columnVisibleIds: T[], pinnedIds: T[]) => void
    // onColumnSaveAs?: (columnSortedIds: T[], columnVisibleIds: T[], pinnedIds: T[]) => void
}
type T = string

function ConfigManageColumns(props: PropsT) {
    const [css, theme] = useStyletron()
    // const locale = React.useContext(LocaleContext)
    const [isOpen, setIsOpen] = React.useState(true)
    const [query, setQuery] = React.useState('')

    // const handleClose = React.useCallback(() => {
    //     setIsOpen(false)
    //     setHighlightIndex(-1)
    //     setQuery('')
    // }, [])

    // const filterableColumns = React.useMemo(() => {
    //     return props.columns.filter((column) => {
    //         return column.filterable && !props.filters.has(column.title)
    //     })
    // }, [props.columns, props.filters])

    const ref = useRef(null)
    const { columns } = props
    const columnAllIds = useMemo(() => {
        return columns.map((v) => v.key as string)
    }, [columns])
    const matchedColumns = React.useMemo(() => {
        return columns.filter((column) => matchesQuery(column.title, query)) ?? []
    }, [columns, query])
    const columnMatchedIds = useMemo(() => {
        return matchedColumns.map((v) => v.key) ?? []
    }, [matchedColumns])

    const {
        selectedIds,
        sortedIds,
        pinedIds,
        handleSelectMany,
        handleSelectNone,
        handleSelectOne,
        handleOrderChange,
        handlePinOne,
        handleEmpty,
    } = useSelection<T>({
        initialSelectedIds: props.config?.selectIds ?? [],
        initialPinedIds: props.config?.pinnedIds ?? [],
        initialSortedIds: props.config?.sortedIds ?? [],
    })

    const dndData = useMemo(() => {
        const DnDCell = ({ column, pined }: { column: ColumnT; pined: boolean }) => {
            const [hoverable] = useHover((hoverd) => {
                if (!column) return <></>

                return (
                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            paddingLeft: '10px',
                            paddingRight: '9px',
                            height: '32px',
                            cursor: 'pointer',
                            willChange: 'transform',
                            flexWrap: 'nowrap',
                            justifyContent: 'space-between',
                            background: hoverd ? '#F0F4FF' : '#FFFFFF',
                        }}
                    >
                        <LabelSmall>{column.title}</LabelSmall>
                        <div>
                            {(pined || hoverd) && (
                                <Button
                                    overrides={{
                                        BaseButton: {
                                            style: {
                                                'paddingLeft': '7px',
                                                'paddingRight': '7px',
                                                'color': pined ? 'rgba(2,16,43,0.80)' : 'rgba(2,16,43,0.20)',
                                                ':hover': {
                                                    background: 'transparent',
                                                    color: pined ? '#02102B' : 'rgba(2,16,43,0.50)',
                                                },
                                            },
                                        },
                                    }}
                                    as='transparent'
                                    onClick={() => handlePinOne(column.key as string)}
                                >
                                    <AiOutlinePushpin size={16} />
                                </Button>
                            )}
                            <Button
                                overrides={{
                                    BaseButton: {
                                        style: {
                                            paddingLeft: '7px',
                                            paddingRight: '7px',
                                            color: 'rgba(2,16,43,0.40)',
                                        },
                                    },
                                }}
                                as='transparent'
                                onClick={() => handleSelectOne(column.key as string)}
                            >
                                <RiDeleteBin6Line size={16} />
                            </Button>
                        </div>
                    </div>
                )
            })

            return hoverable
        }

        return selectedIds.map((id) => {
            const column = columns.find((v) => v.key === id)

            if (!column) return { id, text: <></> }
            return {
                id: column?.key as string,
                // @ts-ignore
                text: <DnDCell column={column} pined={pinedIds.includes(id)} />,
            }
        })
    }, [selectedIds, pinedIds, columns, handlePinOne, handleSelectOne])

    const handleSave = useCallback(() => {
        props.onColumnSave?.(sortedIds, selectedIds, pinedIds)
    }, [props, selectedIds, sortedIds, pinedIds])

    return (
        <div ref={ref}>
            <Button
                onClick={() => setIsOpen(!isOpen)}
                shape={SHAPE.pill}
                size={SIZE.compact}
                as='link'
                startEnhancer={() => <Icon />}
                overrides={{
                    BaseButton: {
                        style: {
                            marginLeft: theme.sizing.scale500,
                            marginBottom: theme.sizing.scale500,
                        },
                    },
                }}
            >
                Manage Columns
            </Button>
            {ref.current && (
                <Drawer
                    size='520px'
                    isOpen={isOpen}
                    autoFocus
                    showBackdrop={false}
                    onClose={() => setIsOpen(false)}
                    mountNode={document.body || ref.current}
                    overrides={{
                        Root: {
                            style: {
                                zIndex: '102',
                                margin: 0,
                            },
                        },
                        DrawerContainer: {
                            style: {
                                borderRadius: '0',
                                boxSizing: 'border-box',
                                padding: '0px 0 10px',
                                boxShadow: '0 4px 14px 0 rgba(0, 0, 0, 0.3)',
                                margin: 0,
                            },
                        },
                        DrawerBody: {
                            style: {
                                marginLeft: 0,
                                marginRight: 0,
                                marginTop: 0,
                                marginBottom: 0,
                            },
                        },
                    }}
                >
                    <div
                        className={css({
                            display: 'flex',
                            flexDirection: 'column',
                            height: '56px',
                            lineHeight: '56px',
                            borderBottom: '1px solid #EEF1F6',
                            paddingLeft: '20px',
                            marginBottom: '20px',
                        })}
                    >
                        Manage Columns
                    </div>
                    <div
                        className={css({
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '20px',
                            height: 'calc(100% - 76px)',
                            paddingLeft: '20px',
                            paddingRight: '20px',
                        })}
                    >
                        <div
                            className={css({
                                width: '280px',
                            })}
                        >
                            <Input
                                startEnhancer={<Search size='18px' />}
                                value={query}
                                // @ts-ignore
                                onChange={(event) => setQuery(event.target.value)}
                            />
                        </div>
                        <div
                            className={css({
                                flex: 1,
                                border: '1px solid #CFD7E6',
                                borderRadius: '4px',
                                display: 'flex',
                            })}
                        >
                            {/* All columns edit */}
                            <div
                                className={css({
                                    borderRight: '1px solid #CFD7E6',
                                    flex: '1 0 50%',
                                })}
                            >
                                <div
                                    className={css({
                                        display: 'flex',
                                        height: '42px',
                                        borderBottom: '1px solid #FFE1F6',
                                        marginBottom: '8px',
                                        fontSize: '14px',
                                        marginLeft: '10px',
                                        marginRight: '9px',
                                        alignItems: 'center',
                                        gap: '9px',
                                    })}
                                >
                                    <Checkbox
                                        checked={selectedIds.length === columns.length}
                                        onChange={(e) =>
                                            // @ts-ignore
                                            e.target?.checked ? handleSelectMany(columnAllIds) : handleSelectNone()
                                        }
                                    />
                                    <LabelSmall>All columns</LabelSmall>
                                    <span
                                        style={{
                                            marginLeft: '-5px',
                                            color: 'rgba(2,16,43,0.40)',
                                        }}
                                    >
                                        ({selectedIds.length}/{columns.length})
                                    </span>
                                </div>
                                <div
                                    className={css({
                                        display: 'flex',
                                        flexDirection: 'column',
                                    })}
                                >
                                    {columnAllIds.map((id) => {
                                        if (!columnMatchedIds.includes(id)) {
                                            return null
                                        }
                                        const column = columns.find((v) => v.key === id)
                                        if (!column) return null

                                        return (
                                            <div
                                                key={id}
                                                style={{
                                                    paddingLeft: '10px',
                                                    paddingRight: '9px',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    gap: '9px',
                                                    height: '32px',
                                                }}
                                            >
                                                <Checkbox
                                                    checked={selectedIds?.includes(id)}
                                                    onChange={() => handleSelectOne(id)}
                                                />
                                                <LabelSmall>{column.title}</LabelSmall>
                                            </div>
                                        )
                                    })}
                                </div>
                            </div>
                            {/* Visible columns edit */}
                            <div className={css({ flex: '1 0 50%' })}>
                                <div
                                    className={css({
                                        display: 'flex',
                                        height: '42px',
                                        borderBottom: '1px solid #FFE1F6',
                                        marginBottom: '8px',
                                        marginLeft: '10px',
                                        marginRight: '9px',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                    })}
                                >
                                    Visible columns
                                    <span
                                        style={{
                                            marginLeft: '-5px',
                                            color: 'rgba(2,16,43,0.40)',
                                        }}
                                    >
                                        ({selectedIds.length})
                                    </span>
                                    <Button as='link' onClick={handleEmpty}>
                                        empty
                                    </Button>
                                </div>
                                {dndData.length === 0 && (
                                    <div className='flex-column-center'>
                                        <div
                                            style={{
                                                background: '#EEF1F6',
                                                borderRadius: '1px',
                                                width: '64px',
                                                height: '64px',
                                                marginBottom: '20px',
                                                marginTop: '68px',
                                            }}
                                        />
                                        <p
                                            style={{
                                                color: 'rgba(2,16,43,0.40)',
                                                maxWidth: '189px',
                                                textAlign: 'center',
                                            }}
                                        >
                                            Please select a column in the left side
                                        </p>
                                    </div>
                                )}
                                {dndData.length > 0 && (
                                    <DnDContainer onOrderChange={handleOrderChange} data={dndData} />
                                )}
                            </div>
                        </div>
                        <div
                            className={css({
                                display: 'flex',
                                justifyContent: 'flex-end',
                                gap: '20px',
                            })}
                        >
                            <Button kind={KIND.secondary} size={SIZE.mini}>
                                Save AS
                            </Button>
                            <Button onClick={handleSave}>Save</Button>
                        </div>
                    </div>
                </Drawer>
            )}
        </div>
    )
}

export default ConfigManageColumns

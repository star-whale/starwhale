import useSelection from '@starwhale/ui/utils/useSelection'
import Checkbox from '../Checkbox'
import { createUseStyles } from 'react-jss'
import { LabelSmall } from 'baseui/typography'
import classNames from 'classnames'
import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useHover } from 'react-use'
import { ColumnT, ConfigT } from '../base/data-table/types'
import Button from '../Button'
import IconFont from '../IconFont'
import { ReactSortable } from 'react-sortablejs'
import { useDeepEffect } from '@starwhale/core/utils'
import useUnSortedSelection from '../utils/useUnsortedSelection'

const useStyles = createUseStyles({
    transferList: {
        '& .sortable-ghost ': {
            height: '32px',
            overflow: 'hidden',
            boxShadow: ' 0 2px 8px 0 rgba(0,0,0,0.20)',
            zIndex: 100,
        },
    },
    wrapper: {
        'position': 'relative',
        'width': '100%',
        '&:hover': {
            '& $handler': {
                display: 'block',
            },
        },
    },
    handler: {
        position: 'absolute',
        top: '5px',
        width: '30px',
        marginLeft: '-15px',
        left: 'calc(100% - 50px)',
        display: 'none',
        textAlign: 'center',
        zIndex: '99',
        cursor: '-webkit-grabbing',
    },
})

type TransferValueT = Pick<ConfigT, 'selectedIds' | 'pinnedIds' | 'ids'>

type TransferListPropsT = {
    isDragable?: boolean
    operators: TransferValueT & {
        handleOrderChange: (ids: string[], dragId: any) => void
        handleSelectOne: (id: string) => void
        handleSelectMany: (ids: string[]) => void
        handleSelectNone: () => void
        handlePinOne: (id: string) => void
    }
    columns: ColumnT[]
}

export default TransferList

function TransferList({ isDragable = false, columns, ...props }: TransferListPropsT) {
    const styles = useStyles()
    const [dragId, setDragId] = useState(-1)

    const {
        selectedIds = [],
        pinnedIds = [],
        ids = [],
        handleOrderChange,
        handleSelectOne,
        handleSelectMany,
        handleSelectNone,
        handlePinOne,
    } = props.operators

    const DnDCell = ({ column }: { column: ColumnT }) => {
        const [hoverable] = useHover((hoverd) => {
            if (!column) return <></>

            const pined = pinnedIds.includes(column.key)

            return (
                <div className='transfer-list-content-item' title={column.title}>
                    <Checkbox
                        checked={selectedIds?.includes(column.key)}
                        onChange={() => handleSelectOne(column.key as string)}
                        overrides={{
                            Root: {
                                style: {
                                    flex: 1,
                                    height: '100%',
                                },
                            },
                        }}
                    >
                        <LabelSmall $style={{ overflow: 'hidden', lineHeight: '1.2' }} className='line-clamp'>
                            {column.title}
                        </LabelSmall>
                    </Checkbox>
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
                                                backgroundColor: 'transparent',
                                                color: pined ? '#02102B' : 'rgba(2,16,43,0.50)',
                                            },
                                        },
                                    },
                                }}
                                as='transparent'
                                onClick={() => {
                                    handlePinOne(column.key as string)
                                }}
                            >
                                <IconFont size={14} type='pin' />
                            </Button>
                        )}
                    </div>
                </div>
            )
        })

        return hoverable
    }

    const $data = useMemo(() => {
        return columns
            .map((column: any) => {
                return {
                    id: column.key,
                    ...column,
                }
            })
            .sort((a: any, b: any) => {
                return ids.indexOf(a.id) - ids.indexOf(b.id)
            })
    }, [columns, ids])

    const dataRef = useRef<any>()

    const List = useMemo(() => {
        if (isDragable) {
            return (
                <ul className='transfer-list-content-ul'>
                    <ReactSortable
                        delay={0}
                        handle='.handle'
                        list={$data}
                        setList={(newData) => {
                            dataRef.current = newData
                        }}
                        animation={50}
                        onChoose={(args) => setDragId(args.oldIndex as number)}
                        onEnd={() => {
                            if (dataRef.current) {
                                handleOrderChange(
                                    dataRef.current.map((v: any) => v.id),
                                    dragId
                                )
                                dataRef.current = undefined
                            }
                            setDragId(-1)
                        }}
                    >
                        {$data?.map((item: any) => {
                            return (
                                <div
                                    key={item.id}
                                    className={`${styles.wrapper} item`}
                                    style={{
                                        boxShadow: item.chosen ? '0 2px 8px 0 rgba(0,0,0,0.20)' : undefined,
                                        zIndex: item.chosen ? 10 : 0,
                                        cursor: 'pointer',
                                    }}
                                >
                                    <div className={`handle ${styles.handler}`}>
                                        <IconFont type='drag' />
                                    </div>
                                    <DnDCell column={item} />
                                </div>
                            )
                        })}
                    </ReactSortable>
                </ul>
            )
        }
        return (
            <ul className='transfer-list-content-ul'>
                {$data?.map((column: any) => {
                    const id = column.key
                    return (
                        <li key={id} className='transfer-list-content-item' title={column.title}>
                            <Checkbox
                                checked={selectedIds?.includes(id)}
                                onChange={() => handleSelectOne(id)}
                                overrides={{
                                    Root: {
                                        style: {
                                            flex: 1,
                                        },
                                    },
                                }}
                            >
                                <LabelSmall
                                    $style={{ flex: 1, overflow: 'hidden', lineHeight: '1.1' }}
                                    className='line-clamp'
                                >
                                    {column.title}
                                </LabelSmall>
                            </Checkbox>
                        </li>
                    )
                })}
            </ul>
        )
    }, [columns, isDragable, selectedIds])

    return (
        <div className={classNames(styles.transferList, 'transfer-list')}>
            {/* All columns edit */}
            <div className='transfer-list-content'>
                <div className='transfer-list-content-header'>
                    <Checkbox
                        checked={selectedIds.length === $data?.length && selectedIds.length !== 0}
                        onChange={(e) =>
                            (e.target as any)?.checked ? handleSelectMany($data.map((v) => v.key)) : handleSelectNone()
                        }
                    />
                    <LabelSmall>All columns</LabelSmall>
                    <span
                        style={{
                            marginLeft: '-5px',
                            color: 'rgba(2,16,43,0.40)',
                        }}
                    >
                        ({selectedIds.length}/{$data.length})
                    </span>
                </div>
                <div className='transfer-list-content-body'>{List}</div>
            </div>
        </div>
    )
}

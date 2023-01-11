import useSelection from '@starwhale/ui/utils/useSelection'
import Checkbox from '../Checkbox'
import { createUseStyles } from 'react-jss'
import { LabelSmall } from 'baseui/typography'
import classNames from 'classnames'
import { useEffect, useMemo, useState } from 'react'
import { useHover } from 'react-use'
import { ColumnT, ConfigT } from '../base/data-table/types'
import Button from '../Button'
import IconFont from '../IconFont'
import { ReactSortable } from 'react-sortablejs'
import { useDeepEffect } from '@starwhale/core/utils'

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

type TransferValueT = Pick<ConfigT, 'selectedIds' | 'pinnedIds' | 'sortedIds'>

type TransferListPropsT = {
    isDragable?: boolean
    value: TransferValueT
    columns: ColumnT[]
    onChange: (args: TransferValueT) => void
    raw: any[]
}

export default function TransferList({ isDragable = false, columns, value, ...props }: TransferListPropsT) {
    const styles = useStyles()

    const {
        selectedIds,
        sortedIds,
        pinnedIds,
        handleSelectMany,
        handleSelectNone,
        handleSelectOne,
        handlePinOne,
        handleOrderChange,
    } = useSelection({
        initialSelectedIds: value?.selectedIds ?? [],
        initialPinnedIds: value?.pinnedIds ?? [],
        initialSortedIds: value?.sortedIds ?? [],
    })

    useDeepEffect(() => {
        props.onChange?.({ selectedIds, pinnedIds, sortedIds })
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedIds, sortedIds, pinnedIds])

    const [data, setData] = useState<any>(() =>
        props.raw?.map((id) => {
            const column = columns.find((v) => v.key === id)
            if (!column) return { id, text: '' }
            return {
                id: column?.key as string,
                ...column,
            }
        })
    )

    const DnDCell = ({ column }: { column: ColumnT }) => {
        const [hoverable] = useHover((hoverd) => {
            if (!column) return <></>

            const pined = pinnedIds.includes(column.key)

            return (
                <div className='transfer-list-content-item' title={column.title}>
                    <Checkbox
                        checked={selectedIds?.includes(column.key)}
                        onChange={() => handleSelectOne(column.key)}
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
                                onClick={() => handlePinOne(column.key as string)}
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

    useEffect(() => {
        setData(() =>
            props.raw?.map((id) => {
                const column = columns.find((v) => v.key === id)
                if (!column) return { id, text: '' }
                return {
                    id: column?.key as string,
                    ...column,
                }
            })
        )
    }, [columns, props.raw])

    const [dragId, setDragId] = useState(-1)
    const dragSelect = (currentIndex: number) => {
        setDragId(currentIndex)
    }
    const dragUnselect = () => {
        setDragId(-1)
    }

    const List = useMemo(() => {
        if (isDragable) {
            return (
                <ul className='transfer-list-content-ul'>
                    <ReactSortable
                        delay={100}
                        handle='.handle'
                        list={data}
                        setList={(newData) => {
                            handleOrderChange(
                                newData.map((v) => v.id),
                                dragId
                            )
                            setData(newData)
                        }}
                        animation={50}
                        onChoose={(args) => dragSelect(args.oldIndex as number)}
                        onUnchoose={dragUnselect}
                    >
                        {data?.map((item: any) => {
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
                {props.raw?.map((id: any) => {
                    const column = columns.find((v: any) => v.key === id)
                    if (!column) return null

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
    }, [columns, isDragable, selectedIds, props.raw])

    return (
        <div className={classNames(styles.transferList, 'transfer-list')}>
            {/* All columns edit */}
            <div className='transfer-list-content'>
                <div className='transfer-list-content-header'>
                    <Checkbox
                        checked={selectedIds.length === props.raw?.length}
                        onChange={(e) =>
                            (e.target as any)?.checked ? handleSelectMany(props.raw ?? []) : handleSelectNone()
                        }
                    />
                    <LabelSmall>All columns</LabelSmall>
                    <span
                        style={{
                            marginLeft: '-5px',
                            color: 'rgba(2,16,43,0.40)',
                        }}
                    >
                        ({selectedIds.length}/{props.raw?.length})
                    </span>
                </div>
                <div className='transfer-list-content-body'>{List}</div>
            </div>
        </div>
    )
}

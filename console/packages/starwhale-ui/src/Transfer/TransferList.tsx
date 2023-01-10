import useSelection from '@starwhale/ui/utils/useSelection'
import Checkbox from '../Checkbox'
import { createUseStyles } from 'react-jss'
import { LabelSmall } from 'baseui/typography'
import classNames from 'classnames'
import { useMemo } from 'react'
import { useHover } from 'react-use'
import { ColumnT, ConfigT } from '../base/data-table/types'
import { DnDContainer } from '../DnD'

const useStyles = createUseStyles({
    transfer: {
        '&:.transfer-list': {
            flex: 1,
            borderRadius: '4px',
            display: 'flex',
            overflow: 'hidden',
        },
        '& .transfer-list-content': {
            border: '1px solid #CFD7E6',
            flex: '1',
            display: 'flex',
            flexDirection: 'column',
        },
        '& .transfer-list-toolbar': {
            display: 'flex',
            flex: 'none',
            flexDirection: 'column',
            alignSelf: 'center',
            margin: '0 10px',
            verticalAlign: 'middle',
            gap: '20px',
        },
        '& .transfer-list-content-header': {
            display: 'flex',
            height: '42px',
            borderBottom: '1px solid #EEF1F6',
            marginBottom: '8px',
            fontSize: '14px',
            marginLeft: '10px',
            marginRight: '9px',
            alignItems: 'center',
            gap: '9px',
            flex: 'none',
        },
        '& .transfer-list-content-body': {
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
        },
        '& .transfer-list-content-ul': {
            overflow: 'auto',
        },
        '& .transfer-list-content-item': {
            paddingLeft: '10px',
            paddingRight: '9px',
            display: 'flex',
            alignItems: 'center',
            gap: '9px',
            height: '32px',
            willChange: 'transform',
            flexWrap: 'nowrap',
            justifyContent: 'space-between',
        },
    },
})

type TransferValueT = Pick<ConfigT, 'selectedIds' | 'pinnedIds' | 'sortedIds'>

type TransferListPropsT = {
    isDragable?: boolean
    value: TransferValueT
    columns: ColumnT[]
    onChange: (args: TransferValueT) => void
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
        handleOrderChange,
        handlePinOne,
        handleEmpty,
    } = useSelection<T>({
        initialSelectedIds: value?.selectedIds ?? [],
        initialPinnedIds: value?.pinnedIds ?? [],
        initialSortedIds: value?.sortedIds ?? [],
    })

    // useDeepEffect(() => {
    //     console.log('onApply', selectedIds, pinnedIds, sortedIds)
    //     props.onApply?.(selectedIds, pinnedIds, sortedIds)
    //     // eslint-disable-next-line react-hooks/exhaustive-deps
    // }, [selectedIds, sortedIds, pinnedIds])

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
                            flex: 1,
                        }}
                        title={column.title}
                    >
                        <LabelSmall $style={{ flex: 1, overflow: 'hidden', lineHeight: '1.2' }} className='line-clamp'>
                            {column.title}
                        </LabelSmall>
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
                                <IconFont size={14} type='delete' />
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
                text: <DnDCell column={column} pined={pinnedIds.includes(id)} />,
            }
        })
    }, [selectedIds, pinnedIds, columns, handlePinOne, handleSelectOne])

    const List = useMemo(() => {
        if (isDragable) {
            return (
                <ul className='transfer-list-content-ul'>
                    {dndData.length > 0 && <DnDContainer onOrderChange={handleOrderChange} data={dndData} />}
                </ul>
            )
        }
        return (
            <ul className='transfer-list-content-ul'>
                {value.selectedIds?.map((id: any) => {
                    const column = columns.find((v: any) => v.key === id)
                    if (!column) return null

                    return (
                        <li key={id} className='transfer-list-content-item' title={column.title}>
                            <Checkbox checked={selectedIds?.includes(id)} onChange={() => handleSelectOne(id)} />
                            <LabelSmall
                                $style={{ flex: 1, overflow: 'hidden', lineHeight: '1.1' }}
                                className='line-clamp'
                            >
                                {column.title}
                            </LabelSmall>
                        </li>
                    )
                })}
            </ul>
        )
    }, [columns, isDragable, selectedIds, dndData])

    return (
        <div className={classNames('transfer-list', styles.transfer)}>
            {/* All columns edit */}
            <div className='transfer-list-content'>
                <div className='transfer-list-content-header'>
                    <Checkbox
                        checked={selectedIds.length === value.selectedIds?.length}
                        onChange={(e) =>
                            (e.target as any)?.checked ? handleSelectMany(value.selectedIds ?? []) : handleSelectNone()
                        }
                    />
                    <LabelSmall>All columns</LabelSmall>
                    <span
                        style={{
                            marginLeft: '-5px',
                            color: 'rgba(2,16,43,0.40)',
                        }}
                    >
                        ({selectedIds.length}/{value.selectedIds?.length})
                    </span>
                </div>
                <div className='transfer-list-content-body'>{List}</div>
            </div>
        </div>
    )
}

import React, { useCallback, useEffect } from 'react'

// type T = number | string
export interface IUseSelectionPropsT<T = any> {
    initialSelectedIds: T[]
    initialPinedIds: T[]
    initialSortedIds: T[]
    onSelectionChange?: (values: T[]) => void
}

// let count = 0
// let count1 = 0
// let count2 = 0

export default function useSelection<T>(props: IUseSelectionPropsT<T>) {
    const { initialSelectedIds = [], initialPinedIds = [], initialSortedIds = [] } = props
    const [selectedIds, setSelectedIds] = React.useState(new Set(initialSelectedIds))
    const [sortedIds, setSortedIds] = React.useState(new Set(initialSortedIds))
    const [pinedIds, setPinedIds] = React.useState(new Set(initialPinedIds))

    // console.log('【render】', count++, selectedIds, sortedIds, pinedIds)

    useEffect(() => {
        setSelectedIds((prevIds) => {
            const newSelectedIds = Array.from(prevIds)
            const sortedMergeSelectedIds = Array.from(sortedIds).filter((v: T) => prevIds.has(v))
            const sortedSelectedIds = new Set([
                ...sortedMergeSelectedIds.filter((v: T) => newSelectedIds.includes(v)),
                ...newSelectedIds,
            ])

            // console.log('【render effect 1】 ', count1++, sortedSelectedIds)

            return sortedSelectedIds
        })
    }, [sortedIds])

    useEffect(() => {
        setSortedIds((prevIds) => {
            const sortedMergePinedIds = [...Array.from(pinedIds), ...Array.from(prevIds)]
            sortedMergePinedIds.sort((v1, v2) => {
                const index1 = pinedIds.has(v1) ? 1 : -1
                const index2 = pinedIds.has(v2) ? 1 : -1
                return index2 - index1
            })

            // console.log('【render effect 2】 ', count2++, sortedMergePinedIds)

            return new Set(sortedMergePinedIds)
        })
    }, [pinedIds])

    const handleSelectChange = useCallback(
        (next: Set<T>) => {
            const newSelectedIds = Array.from(next)
            const sortedMergeSelectedIds = Array.from(sortedIds).filter((v: T) => next.has(v))
            const sortedSelectedIds = [
                ...sortedMergeSelectedIds.filter((v: T) => newSelectedIds.includes(v)),
                ...newSelectedIds,
            ]

            setSelectedIds(new Set(sortedSelectedIds))
        },
        [sortedIds]
    )

    const handleSelectMany = useCallback(
        (incomings: T[]) => {
            // @ts-ignore
            handleSelectChange(new Set([...selectedIds, ...incomings.map((v) => v)]))
        },
        [handleSelectChange, selectedIds]
    )

    const handleSelectNone = useCallback(() => {
        handleSelectChange(new Set())
    }, [handleSelectChange])

    const handleSelectOne = useCallback(
        (id: T) => {
            if (selectedIds.has(id)) {
                selectedIds.delete(id)
            } else {
                selectedIds.add(id)
            }
            handleSelectChange(new Set(selectedIds))
        },
        [handleSelectChange, selectedIds]
    )

    const handleOrderChange = useCallback(
        (ids: T[]) => {
            // console.log('handleOrderChange', ids)
            const sortedMergeSelectedIds = Array.from(ids).filter((v: T) => selectedIds.has(v))
            setSortedIds(new Set(sortedMergeSelectedIds))

            let noPindedFlag = 0
            Array.from(sortedMergeSelectedIds).forEach((v: T, index) => {
                if (!pinedIds.has(v)) {
                    noPindedFlag = index
                }
                if (noPindedFlag && pinedIds.has(v)) {
                    pinedIds.delete(v)
                }
            })
            setPinedIds(pinedIds)
        },
        [setSortedIds, selectedIds, pinedIds]
    )

    const handlePinOne = useCallback(
        (id: T) => {
            if (pinedIds.has(id)) {
                pinedIds.delete(id)
            } else {
                pinedIds.add(id)
            }
            setPinedIds(new Set(pinedIds))
        },
        [setPinedIds, pinedIds]
    )

    return {
        selectedIds: Array.from(selectedIds),
        setSelectedIds: (ids: T[]) => setSelectedIds(new Set([...ids])),
        handleSelectMany,
        handleSelectNone,
        handleSelectOne,
        sortedIds: Array.from(sortedIds),
        handleOrderChange,
        pinedIds: Array.from(pinedIds),
        handlePinOne,
        handleReset: () => {
            setSelectedIds(new Set(initialSelectedIds))
            setSortedIds(new Set(initialSortedIds))
            setPinedIds(new Set(initialPinedIds))
        },
        handleEmpty: () => {
            setSelectedIds(new Set())
            setSortedIds(new Set())
            setPinedIds(new Set())
        },
    }
}

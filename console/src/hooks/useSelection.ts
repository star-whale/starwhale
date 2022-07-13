import _ from 'lodash'
import React, { useCallback, useEffect } from 'react'
import usePrevious from './usePrevious'

export interface IUseSelectionPropsT<T = any> {
    initialSelectedIds: T[]
    initialPinnedIds: T[]
    initialSortedIds: T[]
    onSelectionChange?: (values: T[]) => void
}

export default function useSelection<T>(props: IUseSelectionPropsT<T>) {
    const { initialSelectedIds = [], initialPinnedIds = [], initialSortedIds = [] } = props
    const [selectedIds, setSelectedIds] = React.useState(new Set(initialSelectedIds))
    const [sortedIds, setSortedIds] = React.useState(new Set(initialSortedIds))
    const [pinnedIds, setPinnedIds] = React.useState(new Set(initialPinnedIds))

    const oldInitialSelectedIds = usePrevious(initialSelectedIds)
    const oldInitialPinnedIds = usePrevious(initialPinnedIds)
    const oldInitialSortedIds = usePrevious(initialSortedIds)
    const firstRender = React.useRef(true)

    useEffect(() => {
        if (
            !_.isEqual(initialSelectedIds, oldInitialSelectedIds) ||
            !_.isEqual(initialSortedIds, oldInitialSortedIds) ||
            !_.isEqual(initialPinnedIds, oldInitialPinnedIds)
        ) {
            // console.log('reset', initialSelectedIds, oldInitialSelectedIds)
            setSelectedIds(new Set(initialSelectedIds))
            setSortedIds(new Set(initialSortedIds))
            setPinnedIds(new Set(initialPinnedIds))
        }
    }, [
        firstRender,
        initialSelectedIds,
        initialPinnedIds,
        initialSortedIds,
        oldInitialSelectedIds,
        oldInitialPinnedIds,
        oldInitialSortedIds,
    ])

    useEffect(() => {
        setSelectedIds((prevIds) => {
            const newSelectedIds = Array.from(prevIds)
            const sortedMergeSelectedIds = Array.from(sortedIds).filter((v: T) => prevIds.has(v))
            const sortedSelectedIds = new Set([
                ...sortedMergeSelectedIds.filter((v: T) => newSelectedIds.includes(v)),
                ...newSelectedIds,
            ])

            return sortedSelectedIds
        })
    }, [sortedIds])

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
        (ids: T[], dragId: T) => {
            const sortedMergeSelectedIds = Array.from(ids).filter((v: T) => selectedIds.has(v))
            setSortedIds(new Set(sortedMergeSelectedIds))
            const $pinnedIds = new Set(pinnedIds)
            const dragIndex = sortedMergeSelectedIds.findIndex((v: T) => v === dragId)
            $pinnedIds.delete(dragId)

            // move pined column to no pined column will auto remove pined status
            const pindedFlag: number[] = []
            Array.from(sortedMergeSelectedIds).forEach((v: T, index) => {
                if ($pinnedIds.has(v)) {
                    pindedFlag.push(index)
                }
            })

            const maxPinedFlag = Math.max(...pindedFlag)
            if (dragIndex > maxPinedFlag) {
                $pinnedIds.delete(dragId)
            } else if (dragIndex < maxPinedFlag) {
                $pinnedIds.add(dragId)
            }

            setPinnedIds($pinnedIds)
        },
        [setSortedIds, selectedIds, pinnedIds]
    )

    const handlePinOne = useCallback(
        (id: T) => {
            if (pinnedIds.has(id)) {
                pinnedIds.delete(id)
            } else {
                pinnedIds.add(id)
            }
            setPinnedIds(new Set(pinnedIds))
            setSortedIds((prevIds) => {
                const sortedMergePinnedIds = [...Array.from(pinnedIds), ...Array.from(prevIds)]
                sortedMergePinnedIds.sort((v1, v2) => {
                    const index1 = pinnedIds.has(v1) ? 1 : -1
                    const index2 = pinnedIds.has(v2) ? 1 : -1
                    return index2 - index1
                })

                return new Set(sortedMergePinnedIds)
            })
        },
        [setPinnedIds, pinnedIds]
    )

    return {
        selectedIds: Array.from(selectedIds),
        setSelectedIds: (ids: T[]) => setSelectedIds(new Set([...ids])),
        handleSelectMany,
        handleSelectNone,
        handleSelectOne,
        sortedIds: Array.from(sortedIds),
        handleOrderChange,
        pinnedIds: Array.from(pinnedIds),
        handlePinOne,
        handleReset: () => {
            setSelectedIds(new Set(initialSelectedIds))
            setSortedIds(new Set(initialSortedIds))
            setPinnedIds(new Set(initialPinnedIds))
        },
        handleEmpty: () => {
            setSelectedIds(new Set())
            setSortedIds(new Set())
            setPinnedIds(new Set())
        },
    }
}

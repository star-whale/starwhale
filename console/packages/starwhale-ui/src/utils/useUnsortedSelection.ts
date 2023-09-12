import { useDeepEffect } from '@starwhale/core/utils'
import React, { useCallback } from 'react'

export interface IUseSelectionPropsT<T = any> {
    initialIds: T[]
    initialSelectedIds: T[]
    initialPinnedIds: T[]
    onSelectionChange?: (values: T[]) => void
}

export default function useUnSortedSelection<T>(props: IUseSelectionPropsT<T>) {
    const { initialSelectedIds = [], initialPinnedIds = [], initialIds = [] } = props
    const [ids, setIds] = React.useState(new Set(initialIds))
    const [selectedIds, setSelectedIds] = React.useState(new Set(initialSelectedIds))
    const [pinnedIds, setPinnedIds] = React.useState(new Set(initialPinnedIds))

    useDeepEffect(() => {
        setIds(new Set(initialIds))
        setSelectedIds(new Set(initialSelectedIds))
        setPinnedIds(new Set(initialPinnedIds))
    }, [initialIds, initialSelectedIds, initialPinnedIds])

    const handleSelectMany = useCallback(
        (incomings: T[]) => {
            setSelectedIds(new Set([...selectedIds, ...incomings]))
        },
        [setSelectedIds, selectedIds]
    )

    const handleSelectNone = useCallback(() => {
        setSelectedIds(new Set())
    }, [setSelectedIds])

    const handleSelectOne = useCallback(
        (id: T) => {
            if (selectedIds.has(id)) {
                selectedIds.delete(id)
            } else {
                selectedIds.add(id)
            }
            setSelectedIds(new Set(selectedIds))
        },
        [setSelectedIds, selectedIds]
    )

    const handleOrderChange = useCallback(
        (newIds: T[], dragId: T, rawIds: T[]) => {
            const sortedMergeIds = Array.from(newIds).filter((v: T) => ids.has(v))
            const $pinnedIds = new Set(pinnedIds)
            const dragIndex = sortedMergeIds.findIndex((v: T) => v === dragId)
            const prevIds = rawIds
            const dragPrevSiblingId = sortedMergeIds[dragIndex - 1]
            const dragPrevSiblingRawIndex = dragIndex > 0 ? prevIds.findIndex((v: T) => v === dragPrevSiblingId) : 0
            const dragPrevRawIndex = prevIds.findIndex((v: T) => v === dragId)

            prevIds.splice(dragPrevRawIndex, 1)
            if (dragPrevSiblingId === undefined) prevIds.unshift(dragId)
            else prevIds.splice(dragPrevSiblingRawIndex + 1, 0, dragId)

            $pinnedIds.delete(dragId)

            // move pined column to no pined column will auto remove pined status
            const pindedFlag: number[] = []
            Array.from(sortedMergeIds).forEach((v: T, index) => {
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
            setIds(new Set(prevIds))
            return {
                pinnedIds: Array.from($pinnedIds),
                ids: prevIds,
                selectedIds: Array.from(selectedIds),
            }
        },
        [setIds, ids, pinnedIds, selectedIds]
    )

    const handlePinOne = useCallback(
        (id: T) => {
            if (pinnedIds.has(id)) {
                pinnedIds.delete(id)
            } else {
                pinnedIds.add(id)
            }
            setPinnedIds(new Set(pinnedIds))

            const prevIds = ids
            Array.from(pinnedIds).forEach((i) => prevIds.delete(i))
            const sortedIds = [...Array.from(pinnedIds), ...Array.from(prevIds)]
            setIds(new Set(sortedIds))
            return { pinnedIds: Array.from(pinnedIds), ids: sortedIds, selectedIds: Array.from(selectedIds) }
        },
        [setPinnedIds, pinnedIds, ids, selectedIds]
    )

    return {
        ids: Array.from(ids),
        selectedIds: Array.from(selectedIds),
        pinnedIds: Array.from(pinnedIds),
        setSelectedIds: (newIds: T[]) => setSelectedIds(new Set([...newIds])),
        handleSelectMany,
        handleSelectNone,
        handleSelectOne,
        handleOrderChange,
        handlePinOne,
        handleReset: () => {
            setSelectedIds(new Set(initialSelectedIds))
            setPinnedIds(new Set(initialPinnedIds))
        },
        handleEmpty: () => {
            setSelectedIds(new Set())
            setPinnedIds(new Set())
        },
    }
}

import React from 'react'
import { useStore } from './useStore'
import { shallow } from 'zustand/shallow'
import { IGridState } from '../types'
import useGirdData from './useGridData'
import { ExtraPropsT, ConfigColumns, StatefulConfigColumns } from '../components/ConfigColumns'
import useGridCurrentView from './useGridCurrentView'

const selector = (state: IGridState) => ({
    onCurrentViewColumnsChange: state.onCurrentViewColumnsChange,
})

function useGridConfigColumns() {
    const { onCurrentViewColumnsChange } = useStore(selector, shallow)
    const { originalColumns } = useGirdData()
    const { currentView } = useGridCurrentView(originalColumns)

    const renderStatefulConfigColumns = React.useCallback(
        (props?: ExtraPropsT) => {
            return (
                <StatefulConfigColumns
                    {...props}
                    view={currentView}
                    columns={originalColumns}
                    onColumnsChange={onCurrentViewColumnsChange}
                />
            )
        },
        [currentView, onCurrentViewColumnsChange, originalColumns]
    )

    const renderConfigColumns = React.useCallback(
        (props: ExtraPropsT) => {
            return (
                <ConfigColumns
                    {...props}
                    // @ts-ignore
                    view={currentView}
                    columns={originalColumns}
                    onColumnsChange={onCurrentViewColumnsChange}
                />
            )
        },
        [currentView, onCurrentViewColumnsChange, originalColumns]
    )

    return {
        renderConfigColumns,
        renderStatefulConfigColumns,
    }
}

export { useGridConfigColumns }
export default useGridConfigColumns

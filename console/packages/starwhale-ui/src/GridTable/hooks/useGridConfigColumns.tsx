import React from 'react'
import { useStore } from './useStore'
import { shallow } from 'zustand/shallow'
import { IGridState } from '../types'
import { ExtraPropsT, ConfigColumns, StatefulConfigColumns } from '../components/ConfigColumns'
import useGridCurrentView from './useGridCurrentView'

const selector = (state: IGridState) => ({
    onCurrentViewColumnsChange: state.onCurrentViewColumnsChange,
    columns: state.columns,
})

function useGridConfigColumns() {
    const { onCurrentViewColumnsChange, columns } = useStore(selector, shallow)
    const { currentView } = useGridCurrentView()

    const renderStatefulConfigColumns = React.useCallback(
        (props?: ExtraPropsT) => {
            return (
                <StatefulConfigColumns
                    {...props}
                    view={currentView}
                    columns={columns}
                    onColumnsChange={onCurrentViewColumnsChange}
                />
            )
        },
        [currentView, onCurrentViewColumnsChange, columns]
    )

    const renderConfigColumns = React.useCallback(
        (props: ExtraPropsT) => {
            return (
                <ConfigColumns
                    {...props}
                    // @ts-ignore
                    view={currentView}
                    columns={columns}
                    onColumnsChange={onCurrentViewColumnsChange}
                />
            )
        },
        [currentView, onCurrentViewColumnsChange, columns]
    )

    return {
        renderConfigColumns,
        renderStatefulConfigColumns,
    }
}

export { useGridConfigColumns }
export default useGridConfigColumns

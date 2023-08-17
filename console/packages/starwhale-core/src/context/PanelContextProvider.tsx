import React, { Context, createContext, useContext } from 'react'
import { EventBus } from '../events/types'
import { ColumnSchemaDesc, tablesOfEvaluation } from '../datastore'

export type PanelContextType = {
    eventBus: EventBus
    evalSelectData: any[]
}
type PanelContextProviderProps = {
    value: any
    children: React.ReactNode
}

export const PanelContext: Context<PanelContextType> = createContext({} as PanelContextType)

export function getTableRecordMap(evalSelectData: any) {
    if (!evalSelectData) return {}
    const recordMap = {}
    Object.values(evalSelectData).forEach((value: any) => {
        const { records, summaryTableName } = value || {}
        recordMap[summaryTableName] = records
    })
    return recordMap
}

export function getTableDistinctColumnTypes(evalSelectData: any) {
    if (!evalSelectData) return []

    const newColumnType: ColumnSchemaDesc[] = []
    const columnTypeMap = new Map()
    Object.values(evalSelectData).forEach((value: any) => {
        const { columnTypes } = value || {}
        columnTypes.forEach((columnType: any) => {
            if (columnTypeMap.has(columnType.name)) return
            columnTypeMap.set(columnType.name, columnType)
            newColumnType.push(columnType)
        })
    })
    return newColumnType
}

export function getPrefixes(evalSelectData: any) {
    if (!evalSelectData) return undefined
    const allPrefix: any = []
    Object.values(evalSelectData).forEach((item: any) => {
        allPrefix.push({
            prefix: `${item?.project?.name}`,
            name: item?.summaryTableName,
        })
        item?.rowSelectedIds.forEach((id) => {
            allPrefix.push({
                prefix: `${item?.project?.name}`,
                name: tablesOfEvaluation(item.projectId, id),
            })
        })
    })
    return allPrefix
}

export const usePanelContext = () => useContext(PanelContext)
export const usePanelDatastore = () => {
    const { evalSelectData } = useContext(PanelContext)
    return {
        // eslint-disable-next-line
        getTableRecordMap: React.useCallback(() => getTableRecordMap(evalSelectData), [evalSelectData]),
        // eslint-disable-next-line
        getTableDistinctColumnTypes: React.useCallback(
            () => getTableDistinctColumnTypes(evalSelectData),
            [evalSelectData]
        ),
        getPrefixes: React.useCallback(() => getPrefixes(evalSelectData), [evalSelectData]),
    }
}

export function PanelContextProvider({ children, value }: PanelContextProviderProps) {
    return <PanelContext.Provider value={value}>{children}</PanelContext.Provider>
}

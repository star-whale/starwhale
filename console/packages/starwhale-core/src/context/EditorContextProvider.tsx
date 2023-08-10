import React, { Context, createContext, useContext } from 'react'
import { createCustomStore } from '../store/store'
import { EventBus } from '../events/types'

export type StoreType = ReturnType<typeof createCustomStore>

export type EditorContextType = {
    store: StoreType
    eventBus: EventBus
    dynamicVars: Record<string, any>
    tables: string[]
}
type EditorContextProviderProps = {
    value: any
    children: React.ReactNode
}

export const EditorContext: Context<EditorContextType> = createContext({} as EditorContextType)

export const useEditorContext = () => useContext(EditorContext)
export const useEditorTables = () => useContext(EditorContext).tables

export default function EditorContextProvider({ children, value }: EditorContextProviderProps) {
    return <EditorContext.Provider value={value}>{children}</EditorContext.Provider>
}

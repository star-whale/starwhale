import React, { Context, createContext, useContext } from 'react'
import { createCustomStore } from '../store/store'
import { EventBus } from '../events/types'

export type EditorContextType = {
    store: ReturnType<typeof createCustomStore>
    eventBus: EventBus
}
type EditorContextProviderProps = {
    value: any
    children: React.ReactNode
}

export const EditorContext: Context<EditorContextType> = createContext({} as EditorContextType)

export const useEditorContext = () => useContext(EditorContext)

export default function EditorContextProvider({ children, value }: EditorContextProviderProps) {
    return <EditorContext.Provider value={value}>{children}</EditorContext.Provider>
}

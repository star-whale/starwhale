import React from 'react'

export interface ISidebarContextProps {
    expanded: boolean | undefined
    setExpanded: (expanded: boolean) => void
}

export const SidebarContext = React.createContext<ISidebarContextProps>({
    expanded: true,
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    setExpanded: () => {},
})

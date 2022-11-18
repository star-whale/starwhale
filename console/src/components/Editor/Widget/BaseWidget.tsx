import React, { Component } from 'react'
import { EditorContext } from '../context/EditorContextProvider'
import { WidgetProps } from './const'

export type WidgetState = Record<string, unknown>

class BaseWidget<T extends WidgetProps, K extends WidgetState> extends Component<T, K> {
    static contextType = EditorContext

    declare context: React.ContextType<typeof EditorContext>
}

export default BaseWidget

import React, { Component } from 'react'
import { EditorContext } from '../context/EditorContextProvider'
import { WidgetProps } from '../types'

class BaseWidgetComponent<T extends WidgetProps, K extends object = any> extends Component<T, K> {
    static contextType = EditorContext

    declare context: React.ContextType<typeof EditorContext>
}

export default BaseWidgetComponent

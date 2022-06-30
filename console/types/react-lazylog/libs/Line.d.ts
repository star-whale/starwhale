/* eslint-disable react/static-property-placement */
/* eslint-disable react/prefer-stateless-function */
/* eslint-disable @typescript-eslint/naming-convention */
/* eslint-disable import/no-extraneous-dependencies */
import { Component, ReactNode, CSSProperties, MouseEventHandler } from 'react'

export interface LineProps {
    data: Array<{ text: string }>
    number: number
    rowHeight: number
    highlight?: boolean
    selectable?: boolean
    style?: CSSProperties
    formatPart?: (text: string) => ReactNode
    onLineNumberClick?: MouseEventHandler<HTMLAnchorElement>

    /**
     * This is never called
     * https://github.com/mozilla-frontend-infra/react-lazylog/issues/18
     */
    onRowClick?: () => any
}

export default class Line extends Component<LineProps> {
    static defaultProps: Partial<LineProps>
}

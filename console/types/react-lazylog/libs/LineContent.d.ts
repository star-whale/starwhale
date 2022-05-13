/* eslint-disable react/static-property-placement */
/* eslint-disable react/prefer-stateless-function */
/* eslint-disable @typescript-eslint/naming-convention */
/* eslint-disable import/no-extraneous-dependencies */
import { Component, ReactNode, CSSProperties } from 'react'

export interface LineContentProps {
    data: Array<{ text: string }>
    number: number
    formatPart?: (text: string) => ReactNode
    style?: CSSProperties
}

export default class LineContent extends Component<LineContentProps> {
    static defaultProps: Partial<LineContentProps>
}

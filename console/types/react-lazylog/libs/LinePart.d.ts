/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable react/static-property-placement */
/* eslint-disable react/prefer-stateless-function */
/* eslint-disable @typescript-eslint/naming-convention */
/* eslint-disable import/no-extraneous-dependencies */
import { Component, ReactNode, CSSProperties } from 'react'

export interface LinePartProps {
    part: { text: string }
    format?: (text: string) => ReactNode
    style?: CSSProperties
}

export default class LinePart extends Component<LinePartProps> {
    static defaultProps: Partial<LinePartProps>
}

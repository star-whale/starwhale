/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable react/static-property-placement */
/* eslint-disable react/prefer-stateless-function */
/* eslint-disable @typescript-eslint/naming-convention */
/* eslint-disable import/no-extraneous-dependencies */
import { Component, ReactNode, CSSProperties, MouseEventHandler } from 'react'

export interface LineNumberProps {
    number: number
    highlight?: boolean
    onClick?: MouseEventHandler<HTMLAnchorElement>
    style?: CSSProperties
}

export default class LinePart extends Component<LineNumberProps> {
    static defaultProps: Partial<LineNumberProps>
}

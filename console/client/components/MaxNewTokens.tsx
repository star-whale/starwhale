import React from 'react'
import { Slider } from './Slider'

interface IMaxLenProps {
    onChange: (num: number) => void
}

export const MaxNewTokens = ({ onChange }: IMaxLenProps) => {
    return (
        <Slider
            label='Max new tokens'
            onChange={onChange}
            description='The maximum number of tokens to generate. The higher this value, the longer the text that will be generated.'
            defaultValue={256}
            max={1024}
            min={0}
            step={1}
        />
    )
}

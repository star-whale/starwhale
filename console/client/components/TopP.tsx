import React from 'react'
import { Slider } from './Slider'

interface ITopPProps {
    onChange: (num: number) => void
}

export const TopP = ({ onChange }: ITopPProps) => {
    return (
        <Slider
            label='Top P'
            onChange={onChange}
            description='The top-p parameter limits the model’s predictions such that the cumulative probability of the tokens generated is always less than p'
            defaultValue={0.8}
            max={1}
            min={0}
            step={0.01}
        />
    )
}

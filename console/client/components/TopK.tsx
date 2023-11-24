import React from 'react'
import { Slider } from './Slider'

interface ITopKProps {
    onChange: (num: number) => void
}

export const TopK = ({ onChange }: ITopKProps) => {
    return (
        <Slider
            label='Top K'
            onChange={onChange}
            description='The top-k parameter limits the model’s predictions to the top k most probable tokens at each step of generation'
            defaultValue={1}
            max={100}
            min={0}
            step={1}
        />
    )
}

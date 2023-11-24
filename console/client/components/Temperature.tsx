import React from 'react'
import { ISliderProps, Slider } from './Slider'

interface ITemperatureProps {
    onChangeTemperature: (temperature: number) => void
}

export const TemperatureSlider = ({ onChangeTemperature }: ITemperatureProps) => {
    const props: ISliderProps = {
        label: 'Temperature',
        description:
            'Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.',
        defaultValue: 0.5,
        onChange: onChangeTemperature,
    }
    return <Slider {...props} />
}

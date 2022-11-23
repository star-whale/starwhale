/// <reference types="react" />
import { InputProps, SIZE } from 'baseui/input';
export interface INumberInputProps {
    value?: number;
    onChange?: (value: number) => void;
    min?: number;
    max?: number;
    step?: number;
    disabled?: boolean;
    type?: 'int' | 'float';
    overrides?: InputProps['overrides'];
    size?: SIZE[keyof SIZE];
}
export default function NumberInput({ value, onChange, min, max, step, disabled, type, overrides, size, }: INumberInputProps): JSX.Element;

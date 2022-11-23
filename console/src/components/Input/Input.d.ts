/// <reference types="react" />
import { InputProps, SIZE } from 'baseui/input';
export interface IInputProps extends InputProps {
    overrides?: InputProps['overrides'];
    size?: SIZE[keyof SIZE];
}
export default function Input({ size, ...props }: IInputProps): JSX.Element;

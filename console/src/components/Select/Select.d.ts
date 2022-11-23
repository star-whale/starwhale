/// <reference types="react" />
import { SelectProps, SIZE } from 'baseui/select';
export interface ISelectProps extends SelectProps {
    overrides?: SelectProps['overrides'];
    size?: SIZE[keyof SIZE];
}
export default function Select({ size, ...props }: ISelectProps): JSX.Element;

/// <reference types="react" />
import { ButtonProps, KIND } from 'baseui/button';
export interface IButtonProps extends ButtonProps {
    as?: 'link' | 'button' | 'transparent' | 'withIcon';
    kind?: KIND[keyof KIND];
    isFull?: boolean;
    className?: string;
}
export default function Button({ isFull, size, kind, as, children, ...props }: IButtonProps): JSX.Element;

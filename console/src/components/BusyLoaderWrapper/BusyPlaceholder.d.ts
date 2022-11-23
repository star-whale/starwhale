import React from 'react';
interface IBusyPlaceholderProps {
    type?: 'spinner' | 'loading' | 'notfound' | 'empty';
    style?: React.CSSProperties;
}
export default function BusyPlaceholder({ type, style }: IBusyPlaceholderProps): JSX.Element;
export {};

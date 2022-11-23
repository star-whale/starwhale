import React from 'react';
import './BusyLoaderWrapper.scss';
export interface IBusyLoaderWrapperProps {
    isLoading: boolean;
    className?: string;
    children?: React.ReactElement | any;
    loaderComponent?: React.ReactElement;
    loaderType?: string;
    loaderConfig?: Record<string, unknown>;
    width?: string;
    height?: string;
    style?: React.CSSProperties;
}
declare function BusyLoaderWrapper({ style, isLoading, className, children, loaderType, loaderConfig, width, height, loaderComponent, }: IBusyLoaderWrapperProps): React.FunctionComponentElement<React.ReactNode> | null;
declare const _default: React.MemoExoticComponent<typeof BusyLoaderWrapper>;
export default _default;

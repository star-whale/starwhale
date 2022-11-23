import React from 'react';
export interface IErrorBoundaryProps {
    fallback?: React.ReactElement;
}
export interface IErrorBoundaryState {
    error: Error | null;
}
declare class ErrorBoundary extends React.Component<IErrorBoundaryProps, IErrorBoundaryState> {
    private _fallback;
    constructor(props: IErrorBoundaryProps);
    static getDerivedStateFromError(error: Error): IErrorBoundaryState;
    componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void;
    render(): React.ReactNode;
}
export default ErrorBoundary;

import React from 'react';
import { FieldProps } from 'rc-field-form/lib/Field';
export interface IFormItemProps extends FieldProps {
    label?: React.ReactNode;
    required?: boolean;
    style?: React.CSSProperties;
    positive?: '';
}
export declare const FormItem: ({ label: label_, required, style, children, positive, ...restProps }: IFormItemProps) => JSX.Element;

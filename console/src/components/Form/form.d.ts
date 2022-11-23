import React, { ReactElement, ReactNode } from 'react';
import { FormProps as RcFormProps } from 'rc-field-form/es/Form';
import { FieldProps as RcFieldProps } from 'rc-field-form/es/Field';
import { FieldData, FieldError, Store } from 'rc-field-form/lib/interface';
import { Validator } from './validators';
import { NamePath, Paths, PathType } from './typings';
export declare type FormInstance<S extends {} = Store, K extends keyof S = keyof S> = {
    getFieldValue(name: K): S[K];
    getFieldValue<T extends Paths<S>>(name: T): PathType<S, T>;
    getFieldsValue: (nameList?: NamePath<S>[]) => S;
    getFieldError: (name: NamePath<S>) => string[];
    getFieldsError: (nameList?: NamePath<S>[]) => FieldError[];
    isFieldsTouched(nameList?: NamePath<S>[], allFieldsTouched?: boolean): boolean;
    isFieldsTouched(allFieldsTouched?: boolean): boolean;
    isFieldTouched: (name: NamePath<S>) => boolean;
    isFieldValidating: (name: NamePath<S>) => boolean;
    isFieldsValidating: (nameList: NamePath<S>[]) => boolean;
    resetFields: (fields?: NamePath<S>[]) => void;
    setFields: (fields: FieldData[]) => void;
    setFieldsValue: (value: Partial<S>) => void;
    validateFields: (nameList?: NamePath<K>[]) => Promise<S>;
    submit: () => void;
};
export interface FormProps<S extends {} = Store, V = S> extends Omit<RcFormProps, 'form' | 'onFinish' | 'onValuesChange'> {
    form?: FormInstance<S>;
    initialValues?: Partial<V>;
    onFinish?: (values: V) => void;
    onValuesChange?: (changes: Partial<S>, values: S) => void;
    transformInitialValues?: (payload: Partial<V>) => Partial<S>;
    beforeSubmit?: (payload: S) => V;
}
declare type OmittedRcFieldProps = Omit<RcFieldProps, 'name' | 'dependencies' | 'children' | 'rules'>;
interface BasicFormItemProps<S extends {} = Store> extends OmittedRcFieldProps {
    name?: NamePath<S, 10>;
    children?: ReactElement | ((value: S) => ReactElement);
    validators?: Array<Validator | null> | ((value: S) => Array<Validator | null>);
    label?: ReactNode;
    caption?: ReactNode;
    noStyle?: boolean;
    className?: string;
    required?: boolean;
    style?: React.CSSProperties;
}
declare type Deps<S> = Array<NamePath<S>>;
declare type FormItemPropsDeps<S extends {} = Store> = {
    deps?: Deps<S>;
    children?: ReactElement;
    validators?: Array<Validator | null>;
} | {
    deps: Deps<S>;
    validators: (value: S) => Array<Validator | null>;
} | {
    deps: Deps<S>;
    children: (value: S) => ReactElement;
};
export declare type FormItemProps<S extends {} = Store> = BasicFormItemProps<S> & FormItemPropsDeps<S>;
export interface FormItemClassName {
    item?: string;
    label?: string;
    error?: string;
    touched?: string;
    validating?: string;
    help?: string;
}
export declare function createShouldUpdate(names?: Array<string | number | (string | number)[]>): RcFieldProps['shouldUpdate'];
export declare function createForm<S extends {} = Store>({ itemClassName, ...defaultProps }?: Partial<FormItemProps<S>> & {
    itemClassName?: FormItemClassName;
}): {
    Form: React.ForwardRefExoticComponent<FormProps<S, S> & React.RefAttributes<FormInstance<S, keyof S>>>;
    FormItem: (props_: FormItemProps<S>) => React.FunctionComponentElement<import("./item").IFormItemProps>;
    FormList: React.FunctionComponent<import("rc-field-form/es/List").ListProps>;
    FormProvider: React.FunctionComponent<import("rc-field-form/es/FormContext").FormProviderProps>;
    FormItemLabel: React.FC<{
        label: string;
    }>;
    useForm: () => [FormInstance<S>];
};
export {};

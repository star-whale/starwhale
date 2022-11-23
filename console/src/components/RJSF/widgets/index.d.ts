/// <reference types="react" />
declare const Widgets: {
    SelectWidget: {
        ({ autofocus, disabled, formContext, id, multiple, onBlur, onChange, onFocus, options, placeholder, readonly, schema, value, }: import("@rjsf/utils").WidgetProps<any, import("@rjsf/utils").RJSFSchema, any>): JSX.Element;
        defaultProps: {
            formContext: {};
        };
    };
    TextWidget: ({ disabled, formContext, id, onBlur, onChange, onFocus, options, placeholder, readonly, value, }: import("@rjsf/utils").WidgetProps<any, import("@rjsf/utils").RJSFSchema, any>) => JSX.Element;
};
export default Widgets;

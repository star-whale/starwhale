import { createMachine, assign } from 'xstate'

export type filterEvent =
    | { type: 'RESET' }
    | { type: 'BLUR' }
    | { type: 'FOCUS' }
    | { type: 'FOCUSTARGET'; index: number }
    | { type: 'CONFIRM'; index: number; value: any }
    | { type: 'REMOVE'; index: number }
    | { type: 'INIT'; origins: any[] }

export const filterMachine = createMachine(
    {
        /** @xstate-layout N4IgpgJg5mDOIC5QDMCWAbALmATgOgAccwA3VMAdwGIAxAeQGEBVAZQBUBBAJQHEBRNgG0ADAF1EoAgHtYqTKikA7CSAAeiALQBmAIwAmPABYAHCa0BOXQFZhprYYA0IAJ6ItAdmN5zANktbjdy09Y2FbKwBfCKc0LFw8SDlURSgqLj4WARFxJBBpWXklFXUEDRstPD1PHR1hdz93PV8fJ1cEc3c8Gw6fQ3thHUMmvSiYjGx8RPkUqgY6ADkaAEkuAFlslXykotyS7UNhPC1ggI7jH1rDRxdNHXdDrWFDT2O6vT9I6JBYiYSIJJmc0WK3WOhykhk22Uu00WisXmMeiu5nOwjhNiRrU0VS8A1sV1M52M5y0o2+43iU2SqQAQgAZJhcDa5LaFaGgEqWSrmcxNKw6Kw+YnPa5tLQ+CrucwHYy1SyyvyGMk-Sn-aapejMdjcfhCMSbSFs4q3Kp4O48vQ2A4yvpYhAeAwowVC3ruHQ+CVKr4qyZq6m0RisZkQgoKdlqRB6AJHN5Wd6GKyNd4tG7285dAVXYRxnxo2o+ZUU30AqB-ORUYN5Q1h42lELmPC2GoDYnHULmO36HR4ILlJtWHPCcyFuLF9WEHBSAi4TDOPh+mbpVZ0ABqfErrJrMNKwTNHsTemEvRRVQTnfcnTqudsUeElp0w+9RbL46IU5nc4XqSXq-XYINoY7BytyGEcVwWLK5y5vYIR2hYeAevUhjus8VTZsYnxfIoUgQHAKg+gBUK1hoQS4uYibuIY5j6OYd7GHa2g+FYRzHPUwRRoenxjKOE6kOQFCEUa24aH4hxDhRVE0XRDEYXgGH6NUVhUbRzwjr8VIpIJW7ATu8G9DY+gWIMJjnAxzwIQOVQ+I0jQJp4XrcepX4vlpQERnW1EIQmAxRtRBKmamPgGNmTxBe6Sm0S6amqiWE7vjgs7ziWrnhns9beO61mGZRyEYeePg9h4Jh9DoFiJo+jkxa+xBcGAAC2UgkGAKW1lGFRhPo-LUUFFr0amdwFUElF2KV5FSl6URAA */
        id: 'filter',
        initial: 'preview',
        states: {
            preview: {
                on: {
                    FOCUSTARGET: {
                        target: 'editing',
                        actions: 'setFocusTarget',
                    },
                },
            },

            editing: {
                states: {
                    edit: {
                        always: 'propertyEditing',
                    },

                    propertyEditing: {
                        on: {
                            REMOVE: [
                                {
                                    target: 'preRemove',
                                    cond: 'isAllNone',
                                },
                                {
                                    target: 'propertyEditing',
                                    internal: true,
                                    actions: 'focusRemove',
                                },
                            ],
                        },
                    },

                    preRemove: {},
                },

                initial: 'edit',

                on: {
                    RESET: {
                        target: 'editing',
                        actions: 'reset',
                        internal: true,
                    },

                    CONFIRM: [
                        {
                            target: 'editing',
                            actions: 'focusConfirm',
                            internal: true,
                        },
                        { target: 'preview', cond: 'isNextNotEditable', actions: 'submit' },
                    ],

                    BLUR: {
                        target: 'editing',
                        internal: true,
                        actions: 'blur',
                    },

                    FOCUSTARGET: {
                        target: 'editing',
                        actions: 'setFocusTarget',
                        internal: true,
                    },

                    FOCUS: {
                        target: 'editing',
                        actions: 'focus',
                        internal: true,
                    },
                },
            },
        },
        schema: {
            events: {} as filterEvent,
        },
        context: {
            values: [] as { type: string; value: any; editable?: boolean }[],
            origins: [] as { type: string; value: any; editable?: boolean }[],
            focusTarget: 0,
            focused: false,
        },
        on: {},
        predictableActionArguments: true,
        preserveActionOrder: true,
    },
    {
        guards: {
            isNextNotEditable: (context) => {
                return false

                // return (
                //     // context.focusTarget === context.options.length - 1
                //     // || context.options[context.focusTarget + 1]?.editable === false
                // )
            },
            isAllNone: (context) => {
                return context.values.every((option) => option.value === undefined)
            },
        },
        actions: {
            blur: assign({
                focused: false,
            }),
            focus: assign({
                focused: true,
            }),
            setFocusTarget: assign({
                focused: true,
                focusTarget: (context, event) => event.index,
            }),
            focusRemove: assign({
                focused: true,
                values: (context) => {
                    // truncate value from end, find the last value that exist then set it to undefined
                    const values = [...context.values]
                    for (let i = values.length - 1; i >= 0; i--) {
                        if (values[i].value !== undefined) {
                            values[i].value = undefined
                            break
                        }
                    }
                    return values
                },
                focusTarget: (context) => {
                    return Math.max(context.focusTarget - 1, 0)
                },
            }),
            focusConfirm: assign({
                focused: true,
                values: (context, event) => {
                    return context.values.map((option, index) => {
                        if (index === event.index) {
                            return { ...option, value: event.value }
                        }
                        return option
                    })
                },
                focusTarget: (context) => {
                    return Math.min(context.focusTarget + 1, context.origins.length - 1)
                },
            }),
            submit: assign({
                focused: false,
                values: (context) => {
                    return context.values.map((option, index) => {
                        if (index === context.focusTarget) {
                            return { ...option, value: option.value }
                        }
                        return option
                    })
                },
            }),
            reset: assign({
                focused: false,
                focusTarget: 0,
                values: (context) => {
                    return [...context.origins]
                },
            }),
        },
    }
)

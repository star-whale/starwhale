import { IF_KEY } from '@rjsf/utils'
import { OPERATOR } from '@starwhale/core'
import { createMachine, assign, send, actions } from 'xstate'
const { choose, log } = actions

export type filterEvent =
    | { type: 'FOCUSONLASTEDIT' }
    | { type: 'RESET' }
    | { type: 'BLUR' }
    | { type: 'FOCUS' }
    | { type: 'FOCUSTARGET'; index: number }
    | { type: 'CONFIRM'; index: number; value: any }
    | { type: 'REMOVE'; index: number }
    | { type: 'INIT'; origins: any[] }

const $context = {
    values: [] as { type: string; value: any; editable?: boolean }[],
    origins: [] as { type: string; value: any; editable?: boolean }[],
    focusTarget: 0,
    focused: false,
}

type ContextT = typeof $context

const reset = (context: ContextT) => {
    console.log('reset', context.values, context.origins)
    if (context.values[0]?.value !== context.origins[0]?.value) {
        return [...context.values]
    }
    return [...context.origins]
}

const focusOnLastEdit = (context: ContextT) => {
    const index = context.values.findIndex((v) => !v.value)
    if (index !== -1) return index
    return context.values.length - 1
}

export const filterMachine = createMachine(
    {
        /** @xstate-layout N4IgpgJg5mDOIC5QDMCWAbALmATgOgAccwA3VMAdwGIAxAeQGEBVAZToDkAZAQRYBUAogBEAknwDaABgC6iUAQD2sVJlQKAdnJAAPRAFoALJIDMeAJwB2AIzGzdswYMWLZgDQgAnogPGreAGySZibGkhYAHP4ATFH+BgC+8e5oWLh4kCqo6lBUAEoCLAISMlqKyqoaWroIegCsJnhREVZWYf6WUWb+-u5eCJZ49ZZxxkZWBp1RickY2PgZqtlUDBw0IrkAslKySCBlmZW71Xqj4YNR4RYG-uG1ERa+vfpRvng2l7VxUT5WtS-TIBSc3SEEySwAQpwmLltqUlAdNEdEGYonhJOFjJEolYLGEjFFak8ED8AkEQmEsbEEklAbM0gssjl6Mx+NxcgBxIqw3b7CqI0DHHGmAxmVrRfy1EUSz5Enz+PDhFqY2r1UbtfwAoH00GLJmMVjc+TwvlVRAvM6hCwEql3GLdWWvQLBYyWylxTV0+Y6xkglRUQ17Y1qfk6Z7hMxoxWtKzhDGYoJEqzYvAPVXo35-J0e1JesFQQg4BQEXCYDwCb1LfIbOgANQEAd5wdNNW+eCMcXsdosn0JnjNBlqCtj2Imv0kTkc2eBDOyBaLJbLFZyVdr9asOyN5SbSJq4zwox8ZkVkUCowuRNsAX8FjiVjiVtxtVuU+1eYLYFyYAAtgoSGA8gI1Z1g2QaHAKZotG8kjXsYfzOBiZguheEbdDekoPOiliSJ8iQ0uoCgQHAWhajgcJbmBoY1A8ZxBHcVyip0kjnn2NS+Gc1x3lcLixr4Go0iR75kJQZEIs2ejtJIaJmHRIpJsEzF9HotwKrUSbNJKdjjhYL65rqIkmjuJyXnE9RJrY4wGLGPQsYYFgBCqTTXk03x3OEOm+rqHn6du4EtqKAQDpIZmio4VlEtEaL1Nc2ISiK0GRO5M75kQ844KW5Z5t5FHHBcEaiqhZlXAYMa9n0OLyg8VzhD8th3GYiVLu+n4-n+WUhtUBIGIM452MY3zGLiTFErEkn1MO4Yhb8fGJEAA */
        id: 'filter',
        initial: 'preview',
        states: {
            preview: {
                on: {
                    FOCUSONLASTEDIT: {
                        target: 'editing',
                        actions: 'focusOnLastEdit',
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
                                    actions: 'focusBackword',
                                },
                                {
                                    target: 'propertyEditing',
                                    internal: true,
                                    actions: ['focusRemove', 'focusBackword'],
                                },
                            ],
                        },
                    },

                    preRemove: {
                        on: {
                            REMOVE: [
                                {
                                    internal: true,
                                    actions: 'focusBackword',
                                },
                            ],
                        },
                    },
                },

                initial: 'edit',

                on: {
                    RESET: {
                        target: 'editing',
                        actions: ['reset', 'focusOnLastEdit', 'blur'],
                        internal: true,
                    },

                    CONFIRM: {
                        target: 'editing',
                        actions: ['focusConfirm', 'focusForword'],
                        internal: true,
                    },

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
        context: $context,
        on: {},
        predictableActionArguments: true,
        preserveActionOrder: true,
    },
    {
        guards: {
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
            focusOnLastEdit: assign({
                focused: true,
                focusTarget: focusOnLastEdit,
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
            }),
            focusBackword: assign({
                focused: true,
                focusTarget: (context) => {
                    return Math.max(context.focusTarget - 1, 0)
                },
            }),
            focusForword: assign({
                focused: true,
                focusTarget: (context) => {
                    return Math.min(context.focusTarget + 1, context.values.length - 1)
                },
            }),
            focusConfirm: assign((context, { index, value, callback }) => {
                const next = context.values.map((option, curr) => {
                    if (curr === index) {
                        return { ...option, value }
                    }
                    return option
                })
                // remove value if op does not have value
                const op = next[1].value
                const hasValue = op ? op !== OPERATOR.EXISTS || op !== OPERATOR.NOT_EXISTS : true
                if (!hasValue) {
                    next.splice(-1)
                }

                // submit
                if (next.every((option) => !!option.value)) {
                    console.log('submit', next)
                    callback?.({
                        property: next[0].value,
                        op: next[1].value,
                        value: next[2].value,
                    })
                }

                return {
                    focused: true,
                    values: next,
                }
            }),
            reset: assign({
                focused: false,
                focusTarget: 0,
                values: reset,
            }),
        },
    }
)

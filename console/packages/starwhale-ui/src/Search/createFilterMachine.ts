import { IF_KEY } from '@rjsf/utils'
import { OPERATOR } from '@starwhale/core'
import { createMachine, assign } from 'xstate'

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

const reset = (context: ContextT, { cached }) => {
    console.log(cached)
    if (cached) return [...cached]
    return [...context.values]
}

const focusOnLastEdit = (context: ContextT) => {
    const index = context.values.findIndex((v) => !v.value)
    if (index !== -1) return index
    return context.values.length - 1
}

export const filterMachine = createMachine(
    {
        /** @xstate-layout N4IgpgJg5mDOIC5QDMCWAbALmATgOgAccwA3VMAdwGIAxAeQGEBVAZToDkAZAQRYBUAogBEAknwDaABgC6iUAQD2sVJlQKAdnJAAPRAFoALJIDMeAJwB2AIzGzdswYMWLZgDQgAnogPGreAGySZibGkhYAHP4ATFH+BgC+8e5oWLh4kCqo6lBUAEoCLAISMlqKyqoaWroIegCsJnhREVZWYf6WUWb+-u5eCJZ49ZZxxkZWBp1RickY2PgZqtlUDBw0IrkAslKySCBlmZW71XrGtbWNlrXdtWajnbW9+k0WeOHGFqH+kR-+1rXTIBSc3SEEySwAQpwmLltqUlAdNEdEGYonhJG9IlErBYwkYog9PN5fAEgiEwpjYgkkoDZmkFlkcvRmPxuLkAOJFWG7fYVRGgY7Y0wGMytaL+WrC8VXR4IHz+V4tYzhM4mAztfwAoF00GLRmMVhc+Tw3lVRBRJV4UIWfGU2rW2I9Qmy4mBYLGK0UuKa2nzHUMkEqKiGvbGtR8nRPcJmNHhFqSKzhN5KoIyqxYvAfeqhWNnWJBb2pX1gqCEHAKAi4TAeAR+pb5DZ0ABqAmDPLDppqafMRjOOOVrTTVjcTr0sVeUUTUQMQ4ckjaxkS1PUCggcC0WpwcPK7aRNQ+4TRZjtFmFaeCE5lJ3FeCsgRiiaHV3a-2pG9LpHIFC3CI7enakkPY9T06SQLxHZVXlqNNmglOxJCcAtgXpbJvxNXcTlsAIDHqNNbHGAxE0dPo9BaAwbygk84k6AjhSmV8fQDXVGNQnd+SeEUsJw80RUcQiZWiNF6jVLFxWFSQvg1ejC0Y-0iHLStq1rKAWMONiagnaMRW6axzRPadlVTCx5Q+E9wh8GwjxcRd4iAA */
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
                            REMOVE: {
                                actions: ['focusRemove', 'focusBackword'],
                            },
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
            isSomeNone: (context) => {
                return context.values.some((option) => option.value)
            },
        },
        actions: {
            blur: assign({
                focused: false,
                values: reset,
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

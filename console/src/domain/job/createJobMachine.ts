import { createMachine, assign } from 'xstate'
import { IJobVo, IModelViewVo } from '@/api'

const initialStateName = 'init'

export type JobCreateEvent =
    | { type: 'USEREDITING' }
    | { type: 'APIRERUN' }
    | { type: 'MODELTREEFETCHED' }
    | { type: 'RESET' }
    | { type: 'MODELCHANGED' }
    | { type: 'QUERYMODELID' }
    | { type: 'TEMPLATECHANGED'; data: { templateId: string } }

export const jobCreateMachine = createMachine(
    {
        /** @xstate-layout N4IgpgJg5mDOIC5QCsD2AjAwgJzAQwBcwBiAJQFEBlcgFQG0AGAXUVAAdVYBLAr1AO1YgAHogC0ARgDsADgB0AJgAsDKQDYlMqUrUBWCUoDMAGhABPcTPkLDuhVIkSAnGulSFAXw+m0WXIRIAWQB5ABFyABkaCnIAMVpMAAlyUMYWJBAObl4BIVEESTlDVzUnBRklJSldGVsJGVMLAokFBTkGBiMdLVVO1y8fDBx8Ijk8AFcCVFiuABtZyGIAQQAFAEkKUgBVADk0oSyePkEM-JsnIqkOqqtHNRLGxGcJOUqNXQYZPWrDQwGQXzDAJjSbTOYLCDELbUCihNY0NY7ADi+wyhxyJ1A+UqunaMhsSjsul0akeCEMCheMgMHyMNm0hhc-0B-lGuGw434M3mi2h5Fh8MRKOYB04R1yp0QSnKiicUnl9Q0rScDF0ZIcigMancKm0KqcumZQ1ZYDkXH4PGIIXCESSS2RKVR7DFGLy4hahjkTgNUjKLhkn0MVjJCm97QkHwjAeqrSNfhGpvNltWG35uydmRdxzdzRev10jMMEm1CjUX3lZM0rwjlMZHQY9k0caBoyTBGIAEUtvyAJrWyJrVIitFZiVY92huTqOzajr1KS2NXmKW6C6qr4MYul5QSBhqZsmuTjWBgbDkCBHfhQK1hSJ2h1D9LO7LZyUIPWvapliS-TQUpdNBqCgNnKdiqgwjIGAeCZyJAPDchCN42veSKOsOz7ipiIiIBUbRBi4rjUpSTj1GSYiyHI9RyvKq7dDITLeACxowRMUwIZAABCZiBKgEBgLMayQnyAoIsiGboq+44IPoSjtEYGj3KWKhFpW+JyK45Q1EoxaVJ00HAqxYI8hAXE8XxAmQimmzpuhmYvmO2EIK0UhFHuRgUk41JOL8DTLuS1KKKq3wUqWHRaF4jH8LxcBCCyCaivZWH5GIrRqIouoaFoOgySYfmSNUcjEtSuhXOoLjyp4jFxQZoLsRACWYTmNh5jYhhKE4KilgaxZkhSVKblUhjTrU0b6Wyp6cnVDWum+lK4t6djyR89xlmSaieqUQafCRCgldtY2JhaBDTZJjmSNqXptdqRX4mUpF+UBm7avUNRtbY+5VcxwLHqe56XlAJ0OfkrietptSebIeitWtFwGtSpSuCVBalgdsEXgQU0jolOZlAw4YdPKfqyA46ovMBxYOMUqp2JSqOGXVpnRRZgNJYgwGyV8f4uMBy2+U0mhtKunmrvKFIQYaEVAA */
        id: 'jobCreate',
        initial: initialStateName,
        states: {
            autoFilled: {
                on: {
                    APIRERUN: {
                        target: 'rerunFilled',
                        actions: 'setJob',
                    },

                    USEREDITING: 'userEditing',
                },
            },

            rerunFilled: {
                on: {
                    USEREDITING: 'userEditing',
                },
            },

            init: {
                on: {
                    MODELCHANGED: {
                        target: 'autoFilled',
                    },

                    APIRERUN: {
                        target: 'rerunFilled',
                        actions: 'setJob',
                    },

                    QUERYMODELID: {
                        target: 'autoFilledByModelId',
                        actions: 'setQueryModelId',
                    },
                },
            },

            userEditing: {
                on: {
                    MODELCHANGED: 'editFilled',
                },
            },

            editFilled: {
                on: {
                    MODELCHANGED: {
                        target: 'editFilled',
                        internal: true,
                    },
                },
            },

            autoFilledByModelId: {
                on: {
                    USEREDITING: 'userEditing',
                    APIRERUN: {
                        target: 'rerunFilled',
                        actions: 'setJob',
                    },
                },
            },
        },
        on: {
            RESET: {
                target: 'init',
                actions: {
                    type: 'reset',
                    params: {},
                },
            },
            MODELTREEFETCHED: {
                actions: 'setTree',
            },
        },
        schema: {
            // context: {} as {},
            events: {} as JobCreateEvent,
        },
        context: {
            modelTree: [] as IModelViewVo[],
            job: {} as IJobVo,
            modelId: '',
            modelVersionHandler: '',
        },
        predictableActionArguments: true,
        preserveActionOrder: true,
    },
    {
        actions: {
            setJob: assign({
                // @ts-ignore
                job: (context, event) => event.data,
            }),
            setTree: assign({
                // @ts-ignore
                modelTree: (context, event) => event.data,
            }),
            setQueryModelId: assign({
                // @ts-ignore
                modelId: (context, event) => event.data?.modelId,
                // @ts-ignore
                modelVersionHandler: (context, event) => event.data?.modelVersionHandler,
            }),
            // @ts-ignore
            reset: () => assign({}),
        },
    }
)

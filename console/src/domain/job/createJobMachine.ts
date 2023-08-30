import { createMachine, assign } from 'xstate'
import { IModelTreeSchema } from '../model/schemas/model'
import { IJobSchema } from './schemas/job'

const initialStateName = 'init'

export type JobCreateEvent =
    | { type: 'USEREDITING' }
    | { type: 'APIRERUN' }
    | { type: 'USEREDITING' }
    | { type: 'MODELTREEFETCHED' }
    | { type: 'RESET' }
    | { type: 'MODELCHANGED' }
    | { type: 'QUERYMODELID' }

export const jobCreateMachine = createMachine(
    {
        /** @xstate-layout N4IgpgJg5mDOIC5QCsD2AjAwgJzAQwBcwBiAJQFEBlcgFQG0AGAXUVAAdVYBLAr1AO1YgAHogC0ARgDsADgB0AJgAsDKQDYlMqUrUBWCUoDMAGhABPcTPkLDuhVIkSAnGulSFAXw+m0WXIRIAWQB5ABFyABkaCnIAMVpMAAlyUMYWJBAObl4BIVEESTlDVzUnBRklJSldGVsJGVMLAokFBTkGBiMdLVVO1y8fDBx8Ijk8AFcCVFiuABtZyGIAQQAFAEkKUgBVADk0oSyePkEM-JsnIqkOqqtHNRLGxGcJOUqNXQYZPWrDQwGQXzDAJjSbTOYLCDELbUCihNY0NY7ADi+wyhxyJ1A+UqunaMhsSjsul0akeCEMCheMgMHyMNm0hhc-0B-lGuGw434M3mi2h5Fh8MRKOYB04R1yp0QSnKiicUnl9Q0rScDF0ZIcigMancKm0KqcumZQ1ZYDkXH4PGIIXCESSS2RKVR7DFGLy4hahjkTgNUjKLhkn0MVjJCm97QkHwjAeqrSNfhGpvNltWG35uydmRdxzdzRev10jMMEm1CjUX3lZM0rwjlMZHQY9k0caBoyTBGIAEUtvyAJrWyJrVIitFZiVY92huTqOzajr1KS2NXmKW6C6qr4MYul5QSBhqZsmuTjWBgbDkCBHfhQK1hSJ2h1D9LO7LZyUIPWvapliS-TQUpdNBqCgNnKdiqgwjIGAeCZyJAPDchCN42veSKOsOz7ipiIiIBUbRBi4rjUpSTj1GSYiyHI9RyvKq7dDITLeACxowRMUwIZAABCZiBKgEBgLMayQnyAoIsiGboq+44IPoSjtEYGj3KWKhFpW+JyK45Q1EoxaVJ00HAqxYI8hAXE8XxAmQimmzpuhmYvmO2EIK0UhFHuRgUk41JOL8DTLuS1KKKq3wUqWHRaF4jH8LxcBCCyCaivZWH5GIrRqIouoaFoOgySYfmSNUU4GDi1TUoSGj6aMhnsRACWYTmNh5jYhhKE4KilgaxZkhSVKblUhjTrU0YVaa7KctVtWum+lK4t6djyR89xlmSaieqUQafCRCi6Fc9HDWaFoEBNkmOZI2pes12rElYoaUr5gEvMBxYODINTNbY+6MXFwLHqe56XlAR0OfkrietptSebIehNctFwGtSpSuNtBalntcEEONI6JTmZQMOGHTyn6sgOOqD2btqP5qKqdiUntVXgpx3HRRZgNJYgwGyV8f4uMBC13VKamrp5q7yhSEGGhFQA */
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
            modelTree: [] as IModelTreeSchema[],
            job: {} as IJobSchema,
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

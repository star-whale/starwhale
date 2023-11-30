import { isMacOS } from '../utils'
import { DEFAULT_INPUT_TEMPLATE, StoreKey } from '../constant'
import { createPersistStore } from '../utils/store'
import { IApiSpec, IExposedLinkVo, IJobVo, IStepSpec } from '@/api/server/data-contracts'

export enum SubmitKey {
    Enter = 'Enter',
    CtrlEnter = 'Ctrl + Enter',
    ShiftEnter = 'Shift + Enter',
    AltEnter = 'Alt + Enter',
    MetaEnter = 'Meta + Enter',
}

export enum Theme {
    Auto = 'auto',
    Dark = 'dark',
    Light = 'light',
}

export const DEFAULT_CONFIG = {
    lastUpdate: Date.now(), // timestamp, to merge state

    submitKey: isMacOS() ? SubmitKey.MetaEnter : SubmitKey.CtrlEnter,
    avatar: '1f603',
    fontSize: 14,
    theme: Theme.Auto as Theme,
    tightBorder: false,
    sendPreviewBubble: true,
    enableAutoGenerateTitle: true,

    disablePromptHint: false,

    dontShowMaskSplashScreen: false, // dont show splash screen when create chat
    hideBuiltinMasks: false, // dont add builtin masks

    customModels: '',
    jobs: [] as IJobVo[],

    modelConfig: {
        model: 'gpt-3.5-turbo',
        temperature: 0.5,
        top_p: 1,
        max_tokens: 4000,
        presence_penalty: 0,
        frequency_penalty: 0,
        sendMemory: true,
        historyMessageCount: 4,
        compressMessageLengthThreshold: 1000,
        enableInjectSystemPrompts: true,
        template: DEFAULT_INPUT_TEMPLATE,
    },
}

export type ChatConfig = typeof DEFAULT_CONFIG

export type ModelConfig = ChatConfig['modelConfig']

export function limitNumber(x: number, min: number, max: number, defaultValue: number) {
    if (Number.isNaN(x)) {
        return defaultValue
    }

    return Math.min(max, Math.max(min, x))
}

export const ModalConfigValidator = {
    model(x: string) {
        return x
    },
    max_tokens(x: number) {
        return limitNumber(x, 0, 512000, 1024)
    },
    presence_penalty(x: number) {
        return limitNumber(x, -2, 2, 0)
    },
    frequency_penalty(x: number) {
        return limitNumber(x, -2, 2, 0)
    },
    temperature(x: number) {
        return limitNumber(x, 0, 1, 1)
    },
    top_p(x: number) {
        return limitNumber(x, 0, 1, 1)
    },
}

export enum InferenceType {
    llm_chat = 'llm_chat',
    web_handler = 'web_handler',
}
export interface IInference {
    job: IJobVo
    stepSpec: IStepSpec
    apiSpec?: IApiSpec
    exposedLink: IExposedLinkVo
    type: InferenceType
}

export const useServingConfig = createPersistStore(
    { ...DEFAULT_CONFIG },
    (set, get) => ({
        reset() {
            set(() => ({ ...DEFAULT_CONFIG }))
        },

        setJobs(newJobs: IJobVo[]) {
            if (!newJobs || newJobs.length === 0) {
                return
            }

            set(() => ({
                jobs: newJobs,
            }))
        },

        /**
        -   name: serving
            concurrency: 1
            replicas: 1
            expose: 8080
            virtual: true
            job_name: serving
            show_name: virtual handler for model serving
            service_spec:
                version: 0.0.2
                apis:
                - uri: online_eval
                    inference_type: llm_chat
                    components:
                    - name: temperature
                        component_spec_value_type: FLOAT
                    - name: user_input
                        component_spec_value_type: STRING
                    - name: history
                        component_spec_value_type: LIST
            
            "exposedLinks": [
                {
                    "type": "WEB_HANDLER",
                    "name": "virtual handler for model serving",
                    "link": "/gateway/task/311/8080/"
                }
            ],

         */
        getServings(): IInference[] {
            const { jobs } = get()
            const servings = [] as IInference[]
            jobs.forEach((job) => {
                const stepSpec = job.model.version.stepSpecs.find(
                    (s) => s.name === job.jobName && job.jobName === 'serving'
                )
                const apiSpec = stepSpec?.service_spec?.apis?.[0]
                const exposedLink = job.exposedLinks.find((l) => l.type === 'WEB_HANDLER')
                if (!stepSpec || !exposedLink) return
                servings.push({
                    job,
                    stepSpec,
                    apiSpec,
                    exposedLink,
                    type: (apiSpec?.inference_type as InferenceType) ?? InferenceType.web_handler,
                })
            })
            return servings
        },
    }),
    {
        name: StoreKey.Config,
        version: 1,
        migrate(persistedState) {
            const state = persistedState as ChatConfig
            return state as any
        },
    }
)

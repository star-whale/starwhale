export enum InferenceType {
    LLM_CHAT = 'llm_chat',
}

export interface IComponentSpecSchema {
    name: string
    type: string
}

export interface IApiSchema {
    uri: string
    inference_type: InferenceType
    components_hint: IComponentSpecSchema[]
}

export interface ISpecSchema {
    title?: string
    description?: string
    version: string
    apis: IApiSchema[]
}

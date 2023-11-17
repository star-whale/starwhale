export enum InferenceType {
    TEXT_TO_TEXT = 'text_to_text',
    TEXT_TO_IMAGE = 'text_to_image',
    TEXT_TO_AUDIO = 'text_to_audio',
    TEXT_TO_VIDEO = 'text_to_video',
    QUESTION_ANSWERING = 'question_answering',
}

export interface IApiSchema {
    uri: string
    inference_type: InferenceType
}

export interface ISpecSchema {
    title?: string
    description?: string
    version: string
    apis: IApiSchema[]
}

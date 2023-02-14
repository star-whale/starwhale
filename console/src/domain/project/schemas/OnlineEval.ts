interface IOnlineEvalEvent {
    eventTimeInMs: number
    count: number
    reason: string
    message: string
}

export interface IOnlineEvalStatusSchema {
    progress?: number
    events?: IOnlineEvalEvent[]
}

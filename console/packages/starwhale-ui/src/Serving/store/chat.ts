import { ModelConfig, ModelType, useServingConfig, IInference } from './config'
// import { createEmptyMask, Mask } from './mask'
import { DEFAULT_INPUT_TEMPLATE, DEFAULT_SYSTEM_TEMPLATE, StoreKey } from '../constant'
import { prettyObject } from '../utils/format'
import { nanoid } from 'nanoid'
import { createPersistStore } from '../utils/store'
import Locale from '@/i18n'
import axios from 'axios'

export const ROLES = ['system', 'user', 'assistant']
export type MessageRole = (typeof ROLES)[number]

export interface RequestMessage {
    role: MessageRole
    content: string
}

export type ChatMessage = RequestMessage & {
    date: string
    streaming?: boolean
    isError?: boolean
    id: string
    model?: ModelType
}

export interface IBackEndMessage {
    role: 'user' | 'bot'
    content: string
}

export interface ILLMChatQuerySchema {
    user_input: string
    history: IBackEndMessage[]
    confidence?: number
    top_k?: number
    top_p?: number
    temperature?: number
    max_new_tokens?: number
}

export interface IComponentSpecSchema {
    name: string
    type: string
}

export function trimTopic(topic: string) {
    return topic.replace(/[，。！？”“"、,.!?]*$/, '')
}

export function createMessage(override: Partial<ChatMessage>): ChatMessage {
    return {
        id: nanoid(),
        date: new Date().toLocaleString(),
        role: 'user',
        content: '',
        ...override,
    }
}

export interface ChatStat {
    tokenCount: number
    wordCount: number
    charCount: number
}

export interface ChatSession {
    id: string
    topic: string

    memoryPrompt: string
    messages: ChatMessage[]
    stat: ChatStat
    lastUpdate: number
    lastSummarizeIndex: number
    clearContextIndex?: number

    // mask: Mask
    mask: any
    serving: IInference
}

export const DEFAULT_TOPIC = Locale.DefaultTopic
export const BOT_HELLO: ChatMessage = createMessage({
    role: 'assistant',
    content: Locale.BotHello,
})

function getLang() {
    return Locale.language
}

function createEmptySession(id): ChatSession {
    return {
        id: id ?? nanoid(),
        topic: DEFAULT_TOPIC,
        memoryPrompt: '',
        messages: [],
        stat: {
            tokenCount: 0,
            wordCount: 0,
            charCount: 0,
        },
        lastUpdate: Date.now(),
        lastSummarizeIndex: 0,

        mask: {}, // createEmptyMask(),
        serving: null,
    }
}

function countMessages(msgs: ChatMessage[]) {
    return msgs.reduce((pre, cur) => pre + estimateTokenLength(cur.content), 0)
}

function fillTemplateWith(input: string, modelConfig: ModelConfig) {
    const cutoff = ''

    const vars = {
        cutoff,
        model: modelConfig.model,
        time: new Date().toLocaleString(),
        lang: getLang(),
        input,
    }

    let output = modelConfig.template ?? DEFAULT_INPUT_TEMPLATE

    // must contains {{input}}
    const inputVar = '{{input}}'
    if (!output.includes(inputVar)) {
        output += '\n' + inputVar
    }

    Object.entries(vars).forEach(([name, value]) => {
        output = output.replaceAll(`{{${name}}}`, value)
    })

    return output
}

function hasComponent(name: string, components: IComponentSpecSchema[]) {
    return !!components.find((i) => i.name === name)
}

const DEFAULT_CHAT_STATE = {
    sessions: [] as ChatSession[],
    currentSessionIndex: 0,
}

export const useChatStore = createPersistStore(
    DEFAULT_CHAT_STATE,
    (set, _get) => {
        function get() {
            return {
                ..._get(),
                ...methods,
            }
        }

        const methods = {
            clearSessions() {
                set(() => ({
                    sessions: [],
                    currentSessionIndex: 0,
                }))
            },

            selectSession(index: number) {
                set({
                    currentSessionIndex: index,
                })
            },

            moveSession(from: number, to: number) {
                set((state) => {
                    const { sessions, currentSessionIndex: oldIndex } = state

                    // move the session
                    const newSessions = [...sessions]
                    const session = newSessions[from]
                    newSessions.splice(from, 1)
                    newSessions.splice(to, 0, session)

                    // modify current session id
                    let newIndex = oldIndex === from ? to : oldIndex
                    if (oldIndex > from && oldIndex <= to) {
                        newIndex -= 1
                    } else if (oldIndex < from && oldIndex >= to) {
                        newIndex += 1
                    }

                    return {
                        currentSessionIndex: newIndex,
                        sessions: newSessions,
                    }
                })
            },

            newSession(serving?: IInference, mask?: any) {
                const existSession = get().sessions.find((v) => v.id === serving?.job?.id)

                const session = existSession ?? createEmptySession(serving?.job?.id)

                if (serving) {
                    session.serving = serving
                }

                if (mask) {
                    const config = useServingConfig.getState()
                    const globalModelConfig = config.modelConfig

                    session.mask = {
                        ...mask,
                        modelConfig: {
                            ...globalModelConfig,
                            ...mask.modelConfig,
                        },
                    }
                    session.topic = mask.name
                }

                if (existSession) {
                    set((state) => ({
                        sessions: [...state.sessions],
                    }))
                    return
                }

                set((state) => ({
                    currentSessionIndex: 0,
                    sessions: [session, ...state.sessions],
                }))
            },

            nextSession(delta: number) {
                const n = get().sessions.length
                const limit = (x: number) => (x + n) % n
                const i = get().currentSessionIndex
                get().selectSession(limit(i + delta))
            },

            deleteSession(index: number) {
                const deletingLastSession = get().sessions.length === 1
                const deletedSession = get().sessions.at(index)

                if (!deletedSession) return

                const sessions = get().sessions.slice()
                sessions.splice(index, 1)

                const currentIndex = get().currentSessionIndex
                let nextIndex = Math.min(currentIndex - Number(index < currentIndex), sessions.length - 1)

                if (deletingLastSession) {
                    nextIndex = 0
                    sessions.push(createEmptySession())
                }

                // for undo delete action
                const restoreState = {
                    currentSessionIndex: get().currentSessionIndex,
                    sessions: get().sessions.slice(),
                }

                set(() => ({
                    currentSessionIndex: nextIndex,
                    sessions,
                }))

                // showToast(
                //     Locale.Home.DeleteToast,
                //     {
                //         text: Locale.Home.Revert,
                //         onClick() {
                //             set(() => restoreState)
                //         },
                //     },
                //     5000
                // )
            },

            currentSession() {
                let index = get().currentSessionIndex
                const { sessions } = get()
                if (index < 0 || index >= sessions.length) {
                    index = Math.min(sessions.length - 1, Math.max(0, index))
                    set(() => ({ currentSessionIndex: index }))
                }
                const session = sessions[index]
                return session
            },

            onNewMessage(message: ChatMessage) {
                get().updateCurrentSession((session) => {
                    session.messages = session.messages.concat()
                    session.lastUpdate = Date.now()
                })
                get().updateStat(message)
                get().summarizeSession()
            },

            // updateSession
            onResetMessageByIndex(index, messages: IBackEndMessage[]) {
                get().updateSession(index, (session) => {
                    session.messages = messages
                    session.lastUpdate = Date.now()
                })
                // get().updateStat(message)
                // get().summarizeSession()
            },

            async onUserInput(content: string) {
                const { sessions } = get()
                // const modelConfig = session.mask.modelConfig
                const modelConfig = {}

                const userContent = fillTemplateWith(content, modelConfig)
                console.log('[User Input] after template: ', userContent)

                const promises = sessions.map((session, index) => {
                    const { serving, messages } = session
                    if (!serving) return Promise.resolve()
                    const { exposedLink, apiSpec } = serving
                    const data: ILLMChatQuerySchema = {
                        user_input: content,
                        history: messages,
                    }
                    // if (hasComponent('temperature', api.components_hint)) {
                    //     data.temperature = temperature
                    // }
                    // if (hasComponent('top_k', api.components_hint)) {
                    //     data.top_k = topK
                    // }
                    // if (hasComponent('top_p', api.components_hint)) {
                    //     data.top_p = topP
                    // }
                    // if (hasComponent('max_new_tokens', api.components_hint)) {
                    //     data.max_new_tokens = maxNewTokens
                    // }

                    return axios.post<IBackEndMessage[]>(`${exposedLink.link}api/${apiSpec.uri}`, data).then((res) => {
                        get().onResetMessageByIndex(index, res.data)
                    })
                })

                Promise.all(promises).then(() => {
                    console.log('all')
                })
            },

            getMemoryPrompt() {
                const session = get().currentSession()

                return {
                    role: 'system',
                    content: session.memoryPrompt.length > 0 ? Locale.Prompt.History(session.memoryPrompt) : '',
                    date: '',
                } as ChatMessage
            },

            getMessagesWithMemory() {
                const session = get().currentSession()
                const modelConfig = session.mask.modelConfig
                const clearContextIndex = session.clearContextIndex ?? 0
                const messages = session.messages.slice()
                const totalMessageCount = session.messages.length

                // in-context prompts
                const contextPrompts = session.mask.context.slice()

                // system prompts, to get close to OpenAI Web ChatGPT
                const shouldInjectSystemPrompts = modelConfig.enableInjectSystemPrompts
                const systemPrompts = shouldInjectSystemPrompts
                    ? [
                          createMessage({
                              role: 'system',
                              content: fillTemplateWith('', {
                                  ...modelConfig,
                                  template: DEFAULT_SYSTEM_TEMPLATE,
                              }),
                          }),
                      ]
                    : []
                if (shouldInjectSystemPrompts) {
                    console.log('[Global System Prompt] ', systemPrompts.at(0)?.content ?? 'empty')
                }

                // long term memory
                const shouldSendLongTermMemory =
                    modelConfig.sendMemory &&
                    session.memoryPrompt &&
                    session.memoryPrompt.length > 0 &&
                    session.lastSummarizeIndex > clearContextIndex
                const longTermMemoryPrompts = shouldSendLongTermMemory ? [get().getMemoryPrompt()] : []
                const longTermMemoryStartIndex = session.lastSummarizeIndex

                // short term memory
                const shortTermMemoryStartIndex = Math.max(0, totalMessageCount - modelConfig.historyMessageCount)

                // lets concat send messages, including 4 parts:
                // 0. system prompt: to get close to OpenAI Web ChatGPT
                // 1. long term memory: summarized memory messages
                // 2. pre-defined in-context prompts
                // 3. short term memory: latest n messages
                // 4. newest input message
                const memoryStartIndex = shouldSendLongTermMemory
                    ? Math.min(longTermMemoryStartIndex, shortTermMemoryStartIndex)
                    : shortTermMemoryStartIndex
                // and if user has cleared history messages, we should exclude the memory too.
                const contextStartIndex = Math.max(clearContextIndex, memoryStartIndex)
                const maxTokenThreshold = modelConfig.max_tokens

                // get recent messages as much as possible
                const reversedRecentMessages = []
                for (
                    let i = totalMessageCount - 1, tokenCount = 0;
                    i >= contextStartIndex && tokenCount < maxTokenThreshold;
                    i -= 1
                ) {
                    const msg = messages[i]
                    if (!msg || msg.isError) continue
                    tokenCount += estimateTokenLength(msg.content)
                    reversedRecentMessages.push(msg)
                }

                // concat all messages
                const recentMessages = [
                    ...systemPrompts,
                    ...longTermMemoryPrompts,
                    ...contextPrompts,
                    ...reversedRecentMessages.reverse(),
                ]

                return recentMessages
            },

            updateMessage(sessionIndex: number, messageIndex: number, updater: (message?: ChatMessage) => void) {
                const { sessions } = get()
                const session = sessions.at(sessionIndex)
                const messages = session?.messages
                updater(messages?.at(messageIndex))
                set(() => ({ sessions }))
            },

            resetSession() {
                get().updateCurrentSession((session) => {
                    session.messages = []
                    session.memoryPrompt = ''
                })
            },

            summarizeSession() {
                const config = useServingConfig.getState()
                const session = get().currentSession()

                // remove error messages if any
                const messages = session.messages

                // should summarize topic after chating more than 50 words
                const SUMMARIZE_MIN_LEN = 50
                if (
                    config.enableAutoGenerateTitle &&
                    session.topic === DEFAULT_TOPIC &&
                    countMessages(messages) >= SUMMARIZE_MIN_LEN
                ) {
                    // const topicMessages = messages.concat(
                    //     createMessage({
                    //         role: 'user',
                    //         content: Locale.Prompt.Topic,
                    //     })
                    // )
                    // api.llm.chat({
                    //     messages: topicMessages,
                    //     config: {
                    //         model: getSummarizeModel(session.mask.modelConfig.model),
                    //     },
                    //     onFinish(message) {
                    //         get().updateCurrentSession(
                    //             (session) => (session.topic = message.length > 0 ? trimTopic(message) : DEFAULT_TOPIC)
                    //         )
                    //     },
                    // })
                }

                const modelConfig = session.mask.modelConfig
                const summarizeIndex = Math.max(session.lastSummarizeIndex, session.clearContextIndex ?? 0)
                let toBeSummarizedMsgs = messages.filter((msg) => !msg.isError).slice(summarizeIndex)

                const historyMsgLength = countMessages(toBeSummarizedMsgs)

                if (historyMsgLength > modelConfig?.max_tokens ?? 4000) {
                    const n = toBeSummarizedMsgs.length
                    toBeSummarizedMsgs = toBeSummarizedMsgs.slice(Math.max(0, n - modelConfig.historyMessageCount))
                }

                // add memory prompt
                toBeSummarizedMsgs.unshift(get().getMemoryPrompt())

                const lastSummarizeIndex = session.messages.length

                console.log(
                    '[Chat History] ',
                    toBeSummarizedMsgs,
                    historyMsgLength,
                    modelConfig.compressMessageLengthThreshold
                )

                if (historyMsgLength > modelConfig.compressMessageLengthThreshold && modelConfig.sendMemory) {
                    // api.llm.chat({
                    //     messages: toBeSummarizedMsgs.concat(
                    //         createMessage({
                    //             role: 'system',
                    //             content: Locale.Prompt.Summarize,
                    //             date: '',
                    //         })
                    //     ),
                    //     config: {
                    //         ...modelConfig,
                    //         stream: true,
                    //         model: getSummarizeModel(session.mask.modelConfig.model),
                    //     },
                    //     onUpdate(message) {
                    //         session.memoryPrompt = message
                    //     },
                    //     onFinish(message) {
                    //         console.log('[Memory] ', message)
                    //         session.lastSummarizeIndex = lastSummarizeIndex
                    //     },
                    //     onError(err) {
                    //         console.error('[Summarize] ', err)
                    //     },
                    // })
                }
            },

            updateStat(message: ChatMessage) {
                get().updateCurrentSession((session) => {
                    // session.stat.charCount += message.content.length
                    // TODO: should update chat count and word count
                })
            },

            updateSession(index, updater: (session: ChatSession) => void) {
                const { sessions } = get()
                updater(sessions[index])
                set(() => ({ sessions }))
            },

            updateCurrentSession(updater: (session: ChatSession) => void) {
                const { sessions } = get()
                const index = get().currentSessionIndex
                updater(sessions[index])
                set(() => ({ sessions }))
            },

            clearAllData() {
                localStorage.clear()
                window.location.reload()
            },
        }

        return methods
    },
    {
        name: StoreKey.Chat,
        version: 1,
        migrate(persistedState, version) {
            const state = persistedState as any
            const newState = JSON.parse(JSON.stringify(state)) as typeof DEFAULT_CHAT_STATE
            return newState as any
        },
    }
)

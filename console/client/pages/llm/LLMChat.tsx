import React, { useMemo } from 'react'
import { IApiSchema, IComponentSpecSchema } from '../../schemas/api'
import axios from 'axios'
import { ChatHistory } from '../../components/ChatHistory'
import { ChatInput } from '../../components/ChatInput'
import { IBackEndMessage } from '../../schemas/chat'
import { TemperatureSlider } from '../../components/Temperature'
import { TopK } from '../../components/TopK'
import { TopP } from '../../components/TopP'
import { MaxNewTokens } from '../../components/MaxNewTokens'

export interface ILLMChatProps {
    api: IApiSchema
}

interface ILLMChatQuerySchema {
    user_input: string
    history: IBackEndMessage[]
    confidence?: number
    top_k?: number
    top_p?: number
    temperature?: number
    max_new_tokens?: number
}

function hasComponent(name: string, components: IComponentSpecSchema[]) {
    return !!components.find((i) => i.name === name)
}

export default function LLMChat({ api }: ILLMChatProps) {
    const [messages, setMessages] = React.useState<IBackEndMessage[]>([])
    // TODO, set default values from components_hint
    const [temperature, setTemperature] = React.useState<number>(0.5)
    const [topK, setTopK] = React.useState<number>(1)
    const [topP, setTopP] = React.useState<number>(0.8)
    const [maxNewTokens, setMaxNewTokens] = React.useState<number>(256)

    const postMessage = async (message: string) => {
        const data: ILLMChatQuerySchema = {
            user_input: message,
            history: messages,
        }
        if (hasComponent('temperature', api.components_hint)) {
            data.temperature = temperature
        }
        if (hasComponent('top_k', api.components_hint)) {
            data.top_k = topK
        }
        if (hasComponent('top_p', api.components_hint)) {
            data.top_p = topP
        }
        if (hasComponent('max_new_tokens', api.components_hint)) {
            data.max_new_tokens = maxNewTokens
        }
        return axios.post<IBackEndMessage[]>(`/api/${api.uri}`, data)
    }

    const handleSubmit = async (message: string) => {
        const resp = await postMessage(message)
        setMessages(resp.data)
    }

    const handleTemperatureChange = async (t: number) => {
        setTemperature(t)
    }

    const Temperature = useMemo(() => {
        if (!hasComponent('temperature', api.components_hint)) {
            return <></>
        }
        return <TemperatureSlider onChangeTemperature={handleTemperatureChange} />
    }, [api.components_hint])

    const handleTopKChange = async (t: number) => {
        setTopK(t)
    }

    const TopKInput = useMemo(() => {
        if (!hasComponent('top_k', api.components_hint)) {
            return <></>
        }
        return <TopK onChange={handleTopKChange} />
    }, [api.components_hint])

    const handleTopPChange = async (t: number) => {
        setTopP(t)
    }

    const TopPInput = useMemo(() => {
        if (!hasComponent('top_p', api.components_hint)) {
            return <></>
        }
        return <TopP onChange={handleTopPChange} />
    }, [api.components_hint])

    const handleMaxNewTokensChange = async (t: number) => {
        setMaxNewTokens(t)
    }

    const MaxNewTokensInput = useMemo(() => {
        if (!hasComponent('max_new_tokens', api.components_hint)) {
            return <></>
        }
        return <MaxNewTokens onChange={handleMaxNewTokensChange} />
    }, [api.components_hint])

    return (
        <div className='max-w-[80%] flex flex-col mx-auto items-center justify-center md:px-6 md:py-8 xl:max-w-4xl'>
            {messages.length === 0 && (
                <>
                    {Temperature}
                    {TopKInput}
                    {TopPInput}
                    {MaxNewTokensInput}
                </>
            )}
            <ChatHistory history={messages} />
            <div className='pointer-events-none absolute inset-x-0 bottom-0 z-0 mx-auto flex w-full max-w-3xl flex-col  [&>*]:pointer-events-auto'>
                <div className='mt-4 sm:mt-8 bottom-[56px] left-0 w-full'>
                    <ChatInput onSubmit={handleSubmit} />
                </div>
            </div>
        </div>
    )
}

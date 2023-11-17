import React from 'react'
import { IApiSchema } from './schemas'
import { IconFont, Input } from '@starwhale/ui'
import { IconTypesT } from '@starwhale/ui/IconFont'
import axios from 'axios'

export interface IQuestionAnsweringProps {
    api: IApiSchema
}

interface IChatMessageProps {
    type: 'user' | 'blog'
    message: string
}

const Icon = ({ type }: { type: IconTypesT }) => (
    <div style={{ paddingRight: '20px' }}>
        <IconFont type={type} size={24} />
    </div>
)

const ChatMessage = ({ type, message }: IChatMessageProps) => {
    const clsName = type === 'user' ? 'bg-slate-100' : 'bg-slate-400'
    return (
        <div className={`flex px-4 py-8 ${clsName} sm:px-6`}>
            <Icon type={type} />
            <div className='flex w-full flex-col items-start lg:flex-row lg:justify-between'>
                <p className='max-w-3xl'>{message}</p>
            </div>
        </div>
    )
}

export default function QuestionAnswering({ api }: IQuestionAnsweringProps) {
    const [messages, setMessages] = React.useState<IChatMessageProps[]>([])
    const [input, setInput] = React.useState<string>('')

    const postMessage = async (message: string) => {
        return axios.post<string>(`/api/${api.uri}`, { content: message })
    }

    return (
        <div>
            {messages.map((i, idx) => (
                <ChatMessage key={idx} type={i.type} message={i.message} />
            ))}
            <Input
                onChange={(e) => setInput(e.currentTarget.value)}
                value={input}
                onKeyUp={async (e) => {
                    if (e.key === 'Enter') {
                        setMessages([...messages, { type: 'user', message: input }])
                        const resp = await postMessage(input)
                        setMessages((prev) => [...prev, { type: 'blog', message: resp.data }])
                        setInput('')
                    }
                }}
            />
        </div>
    )
}

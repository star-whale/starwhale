import React from 'react'
import { IBackEndMessage } from '../schemas/chat'
import { IconTypesT } from '@starwhale/ui/IconFont'
import { IconFont } from '@starwhale/ui'

export interface IChatHistoryProps {
    history: IBackEndMessage[]
}

const Icon = ({ type }: { type: IconTypesT }) => (
    <div style={{ paddingRight: '20px' }}>
        <IconFont type={type} size={18} />
    </div>
)

const ChatMessage = ({ role, content }: IBackEndMessage) => {
    return (
        <div className={`flex flex-col ${role === 'bot' ? 'items-start' : 'items-end'}`}>
            <div
                className={`flex items-center ${
                    role === 'bot' ? 'bg-neutral-200 text-neutral-900' : 'bg-blue-200'
                } rounded-xl px-3 py-2 max-w-[80%]`}
                style={{ overflowWrap: 'anywhere' }}
            >
                <Icon type={role === 'bot' ? 'blog' : 'user'} />
                {content}
            </div>
        </div>
    )
}
export const ChatHistory = ({ history }: IChatHistoryProps) => {
    return (
        <div className='mx-auto space-y-5 md:space-y-10 px-3 pt-5 md:pt-12 w-[100%]'>
            <div className='flex flex-col space-y-5'>
                {history.map((i) => (
                    <ChatMessage key={i.content} {...i} />
                ))}
            </div>
        </div>
    )
}

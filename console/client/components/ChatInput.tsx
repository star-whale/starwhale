import React, { useRef } from 'react'

export interface IChatInputProps {
    onSubmit: (message: string) => void
}

export const ChatInput = ({ onSubmit }: IChatInputProps) => {
    const textareaRef = useRef<HTMLTextAreaElement>(null)

    const handleKeyUp = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === 'Enter') {
            e.preventDefault()
            onSubmit(textareaRef.current?.value ?? '')
            textareaRef.current!.value = ''
        }
    }

    return (
        <div className='relative'>
            <textarea
                ref={textareaRef}
                className='min-h-[44px] rounded-lg pl-4 pr-12 py-2 w-full focus:outline-none focus:ring-1 focus:ring-neutral-300 border-2 border-neutral-200'
                style={{ resize: 'none' }}
                placeholder='Type a message...'
                rows={1}
                onKeyUp={handleKeyUp}
            />
        </div>
    )
}

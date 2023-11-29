import React, { useEffect, useRef, useState } from 'react'
import { SubmitKey, useServingConfig } from '../store/config'
import { useDebounce } from 'react-use'
import { useDebounceEffect } from 'ahooks'
import { autoGrowTextArea } from '../utils'
import Button, { ExtendButton } from '@starwhale/ui/Button'
import useScrollToBottom from '../hooks/useScrollToBottom'
import { Textarea } from 'baseui/textarea'

function useSubmitHandler() {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const config = useServingConfig()
    const { submitKey } = config
    const isComposing = useRef(false)

    useEffect(() => {
        const onCompositionStart = () => {
            isComposing.current = true
        }
        const onCompositionEnd = () => {
            isComposing.current = false
        }

        window.addEventListener('compositionstart', onCompositionStart)
        window.addEventListener('compositionend', onCompositionEnd)

        return () => {
            window.removeEventListener('compositionstart', onCompositionStart)
            window.removeEventListener('compositionend', onCompositionEnd)
        }
    }, [])

    const shouldSubmit = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key !== 'Enter') return false
        if (e.key === 'Enter' && (e.nativeEvent.isComposing || isComposing.current)) return false
        return (
            (config.submitKey === SubmitKey.AltEnter && e.altKey) ||
            (config.submitKey === SubmitKey.CtrlEnter && e.ctrlKey) ||
            (config.submitKey === SubmitKey.ShiftEnter && e.shiftKey) ||
            (config.submitKey === SubmitKey.MetaEnter && e.metaKey) ||
            (config.submitKey === SubmitKey.Enter && !e.altKey && !e.ctrlKey && !e.shiftKey && !e.metaKey)
        )
    }

    return {
        submitKey,
        shouldSubmit,
    }
}

function ChatInput({ inputRef, scrollDomToBottom, userInput, setUserInput }) {
    const { submitKey, shouldSubmit } = useSubmitHandler()
    const [inputRows, setInputRows] = useState(2)
    const config = useServingConfig()

    useDebounceEffect(
        () => {
            const rows = inputRef.current ? autoGrowTextArea(inputRef.current) : 1
            const _inputRows = Math.min(5, Math.max(1, rows))
            setInputRows(_inputRows)
        },
        [userInput],
        {
            wait: 100,
            leading: true,
            trailing: true,
        }
    )
    const onInput = (text: string) => {
        setUserInput(text)
    }

    const doSubmit = (userInput: string) => {
        if (userInput.trim() === '') return
        setUserInput('')
        // if (!isMobileScreen) inputRef.current?.focus()
        // setAutoScroll(true)
    }

    // check if should send message
    const onInputKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        // if ArrowUp and no userInput, fill with last input
        // if (e.key === 'ArrowUp' && userInput.length <= 0 && !(e.metaKey || e.altKey || e.ctrlKey)) {
        //     setUserInput(localStorage.getItem(LAST_INPUT_KEY) ?? '')
        //     e.preventDefault()
        //     return
        // }
        if (shouldSubmit(e)) {
            doSubmit(userInput)
            e.preventDefault()
        }
    }

    function scrollToBottom() {
        // setMsgRenderIndex(renderMessages.length - CHAT_PAGE_SIZE);
        scrollDomToBottom()
    }

    return (
        <div className='chat-input-panel-inner w-full border-1 flex flex-1 py-6px px-12px items-end rounded-4px'>
            <textarea
                ref={inputRef}
                className='chat-input w-full h-full'
                // placeholder={Locale.Chat.Input(submitKey)}
                onInput={(e) => onInput(e.currentTarget.value)}
                value={userInput}
                onKeyDown={onInputKeyDown}
                onFocus={scrollToBottom}
                onClick={scrollToBottom}
                rows={inputRows}
                // autoFocus={autoFocus}
                style={{
                    fontSize: config.fontSize,
                }}
            />
            <div className='flex-shrink-0 lh-none'>
                <Button onClick={() => doSubmit(userInput)}>Enter</Button>
            </div>
        </div>
    )
}
export { ChatInput, useSubmitHandler }
export default ChatInput

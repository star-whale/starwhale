import CasecadeResizer from '@starwhale/ui/AutoResizer/CascadeResizer'
import { InferenceType, useServingConfig } from '../store/config'
import { BusyPlaceholder } from '@starwhale/ui/BusyLoaderWrapper'
import JobStatus from '@/domain/job/components/JobStatus'
import Button, { ExtendButton } from '@starwhale/ui/Button'
import { useDomsScrollToBottom } from '../hooks/useScrollToBottom'
import { Fragment, startTransition, useMemo, useRef, useState } from 'react'
import { useDebounceEffect } from 'ahooks'
import useSubmitHandler from '../hooks/useSubmitHandler'
import { autoGrowTextArea } from '../utils'
import { ChatMessage, ChatSession } from '../store/chat'
import { useChatStore as Store } from '@starwhale/ui/Serving/store/chat'
import { nanoid } from 'nanoid'
import { LAST_INPUT_KEY } from '../constant'
import useTranslation from '@/hooks/useTranslation'

export const CHAT_PAGE_SIZE = 15
export const MAX_RENDER_MSG_COUNT = 45
const isMobileScreen = false
type RenderMessage = ChatMessage & { preview?: boolean }
type StoreT = typeof Store

export function createMessage(override: Partial<ChatMessage>): ChatMessage {
    return {
        id: nanoid(),
        date: new Date().toLocaleString(),
        role: 'user',
        content: '',
        ...override,
    }
}

function Chat({
    scrollRef,
    inputRef,
    setAutoScroll,
    useChatStore,
    session,
}: {
    useChatStore: StoreT
    scrollRef: any
    inputRef: any
    setAutoScroll: any
    session: ChatSession
}) {
    // store
    const chatStore = useChatStore()
    const { job } = session.serving

    const isLoading = false

    // init
    const [hitBottom, setHitBottom] = useState(true)

    // session message
    const context: RenderMessage[] = useMemo(() => {
        return session.mask.hideContext ? [] : (session.mask.context ?? [])?.slice()
    }, [session.mask.context, session.mask.hideContext])

    // preview messages
    const renderMessages = useMemo(() => {
        return context.concat(session.messages as RenderMessage[]).concat(
            isLoading
                ? [
                      {
                          ...createMessage({
                              role: 'assistant',
                              content: '……',
                          }),
                          preview: true,
                      },
                  ]
                : []
        )
    }, [context, isLoading, session.messages])
    const [msgRenderIndex, _setMsgRenderIndex] = useState(Math.max(0, renderMessages.length - CHAT_PAGE_SIZE))
    function setMsgRenderIndex(newIndex: number) {
        // eslint-disable-next-line no-param-reassign
        newIndex = Math.min(renderMessages.length - CHAT_PAGE_SIZE, newIndex)
        // eslint-disable-next-line no-param-reassign
        newIndex = Math.max(0, newIndex)
        _setMsgRenderIndex(newIndex)
    }
    const messages = useMemo(() => {
        const endRenderIndex = Math.min(msgRenderIndex + 3 * CHAT_PAGE_SIZE, renderMessages.length)
        return renderMessages.slice(msgRenderIndex, endRenderIndex)
    }, [msgRenderIndex, renderMessages])

    const clearContextIndex =
        (session.clearContextIndex ?? -1) >= 0 ? session.clearContextIndex! + context.length - msgRenderIndex : -1

    //  scroll
    const onChatBodyScroll = (e: HTMLElement) => {
        startTransition(() => {
            const bottomHeight = e.scrollTop + e.clientHeight
            const edgeThreshold = e.clientHeight

            const isTouchTopEdge = e.scrollTop <= edgeThreshold
            const isTouchBottomEdge = bottomHeight >= e.scrollHeight - edgeThreshold
            const isHitBottom = bottomHeight >= e.scrollHeight - (isMobileScreen ? 4 : 10)

            const prevPageMsgIndex = msgRenderIndex - CHAT_PAGE_SIZE
            const nextPageMsgIndex = msgRenderIndex + CHAT_PAGE_SIZE

            if (isTouchTopEdge && !isTouchBottomEdge) {
                setMsgRenderIndex(prevPageMsgIndex)
            } else if (isTouchBottomEdge) {
                setMsgRenderIndex(nextPageMsgIndex)
            }

            setHitBottom(isHitBottom)
            // setAutoScroll(isHitBottom)
        })
    }

    if (!session) return <BusyPlaceholder type='empty' />

    return (
        <div className='chat rounded-4px border-1 border-[#cfd7e6] h-full overflow-hidden flex flex-col pb-15px bg-white'>
            <div className='chat-title flex lh-none h-40px bg-[#eef1f6] px-10px items-center'>
                <ExtendButton
                    disabled={!session?.serving}
                    icon={session?.show ? 'eye' : 'eye_off'}
                    styleas={['menuoption', 'nopadding', 'iconnormal', !session?.serving ? 'icondisable' : undefined]}
                    onClick={() => chatStore.onSessionShowById(job.id, !session?.show)}
                />
                <div className='flex-1 mx-8px font-600'>{job?.modelName}</div>
                <div>
                    <JobStatus status={job?.jobStatus as any} />
                </div>
            </div>

            {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
            <div
                className='chat-body flex-1 overflow-auto overflow-x-hidden p-10px pb-0px relative overscroll-none flex gap-20px flex-col min-w-0 h-full bg-white'
                ref={scrollRef}
                onScroll={(e) => onChatBodyScroll(e.currentTarget)}
                onMouseDown={() => inputRef.current?.blur()}
                onTouchStart={() => {
                    inputRef.current?.blur()
                    setAutoScroll(false)
                }}
            >
                {messages.map((message) => {
                    const isUser = message.role === 'user'
                    // const isContext = i < context.length
                    // const showActions = i > 0 && !(message.preview || message.content.length === 0) && !isContext
                    // const showTyping = message.preview || message.streaming
                    // const shouldShowClearContextDivider = i === clearContextIndex - 1

                    return (
                        <Fragment key={message.id}>
                            <div className={`${isUser ? 'chat-message-user ' : 'chat-message'} `}>
                                <div
                                    className={`chat-message-container  rounded-4px border-1 px-10px py-4px break-words text-wrap inline-block ${
                                        isUser ? 'bg-white' : 'bg-[#EBF1FF]'
                                    }`}
                                >
                                    <div className='chat-message-header'>
                                        <div className='chat-message-avatar'>
                                            <div className='chat-message-edit'>
                                                {/* <IconButton
                                                    icon={<EditIcon />}
                                                    onClick={async () => {
                                                        const newMessage = await showPrompt(
                                                            Locale.Chat.Actions.Edit,
                                                            message.content,
                                                            10
                                                        )
                                                        chatStore.updateCurrentSession((session) => {
                                                            const m = session.mask.context
                                                                .concat(session.messages)
                                                                .find((m) => m.id === message.id)
                                                            if (m) {
                                                                m.content = newMessage
                                                            }
                                                        })
                                                    }}
                                                ></IconButton> */}
                                            </div>
                                        </div>

                                        {/* {showActions && (
                                            <div className={['chat-message-actions']}>
                                                <div className={['chat-input-actions']}>
                                                    {message.streaming ? (
                                                        <ChatAction
                                                            text={Locale.Chat.Actions.Stop}
                                                            icon={<StopIcon />}
                                                            onClick={() => onUserStop(message.id ?? i)}
                                                        />
                                                    ) : (
                                                        <>
                                                            <ChatAction
                                                                text={Locale.Chat.Actions.Retry}
                                                                icon={<ResetIcon />}
                                                                onClick={() => onResend(message)}
                                                            />

                                                            <ChatAction
                                                                text={Locale.Chat.Actions.Delete}
                                                                icon={<DeleteIcon />}
                                                                onClick={() => onDelete(message.id ?? i)}
                                                            />

                                                            <ChatAction
                                                                text={Locale.Chat.Actions.Pin}
                                                                icon={<PinIcon />}
                                                                onClick={() => onPinMessage(message)}
                                                            />
                                                            <ChatAction
                                                                text={Locale.Chat.Actions.Copy}
                                                                icon={<CopyIcon />}
                                                                onClick={() => copyToClipboard(message.content)}
                                                            />
                                                        </>
                                                    )}
                                                </div>
                                            </div>
                                        )} */}
                                    </div>
                                    <div className='chat-message-item'>
                                        {isUser ? (
                                            <div className=''>{message.content}</div>
                                        ) : (
                                            <div>{message.content}</div>
                                        )}
                                    </div>
                                </div>
                            </div>
                            {/* {shouldShowClearContextDivider && <ClearContextDivider />} */}
                        </Fragment>
                    )
                })}
            </div>
        </div>
    )
}

function ChatGroup({ useStore: useChatStore }: { useStore: StoreT }) {
    const chatStore = useChatStore()
    const inputRef = useRef<HTMLTextAreaElement>(null)
    const scroll = useDomsScrollToBottom()
    const [userInput, setUserInput] = useState('')
    const { submitKey, shouldSubmit } = useSubmitHandler()
    const { scrollDomToBottom, setAutoScroll, scrollRefs } = scroll
    const sharedChatProps = { inputRef, ...scroll, userInput, setUserInput }
    const [t] = useTranslation()
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
    const doSubmit = (_userInput: string) => {
        if (_userInput.trim() === '') return
        setUserInput('')
        inputRef.current?.focus()
        setAutoScroll(true)
        chatStore.onUserInput(InferenceType.llm_chat, _userInput)
        localStorage.setItem(LAST_INPUT_KEY, userInput)
    }
    const onInputKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        // if ArrowUp and no userInput, fill with last input
        if (e.key === 'ArrowUp' && userInput.length <= 0 && !(e.metaKey || e.altKey || e.ctrlKey)) {
            setUserInput(localStorage.getItem(LAST_INPUT_KEY) ?? '')
            e.preventDefault()
            return
        }
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
        <div className='chat-group flex flex-col overflow-hidden'>
            <div className='flex overflow-x-auto gap-20px mb-10px text-nowrap flex-nowrap pb-10px'>
                <CasecadeResizer>
                    {chatStore.sessions
                        .filter((session) => session.show && session.serving?.type === InferenceType.llm_chat)
                        .map((v, index) => (
                            <Chat
                                key={v.id}
                                {...sharedChatProps}
                                useChatStore={useChatStore}
                                session={v}
                                scrollRef={(el: HTMLDivElement) => {
                                    scrollRefs.current[index] = el
                                }}
                            />
                        ))}
                </CasecadeResizer>
            </div>
            <div className='chat-input-panel-inner w-full border-1 flex flex-1 py-6px px-12px items-end rounded-4px bg-white'>
                <textarea
                    ref={inputRef}
                    className='chat-input w-full h-full resize-none lh-32px'
                    placeholder={t('ft.online_eval.enter.placeholder', [submitKey])}
                    onInput={(e) => onInput(e.currentTarget.value)}
                    value={userInput}
                    onKeyDown={onInputKeyDown}
                    onFocus={scrollToBottom}
                    onClick={scrollToBottom}
                    rows={inputRows}
                    style={{
                        fontSize: config.fontSize,
                    }}
                />
                <div className='flex-shrink-0 lh-none'>
                    <Button onClick={() => doSubmit(userInput)}>Enter</Button>
                </div>
            </div>
        </div>
    )
}

export default ChatGroup

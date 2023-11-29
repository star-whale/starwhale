import React from 'react'
import ChatGroup from '@starwhale/ui/Serving/components/ChatGroup'
import { InferenceType, useServingConfig } from '@starwhale/ui/Serving/store/config'
import { useChatStore } from '@starwhale/ui/Serving/store/chat'
import _ from 'lodash'
import { useUpdateEffect } from 'react-use'
import { useCreation, useSetState } from 'ahooks'
import SectionAccordionPanel from '@starwhale/ui/Serving/components/SectionAccordionPanel'
import { BusyPlaceholder } from '@starwhale/ui'

export default function FineTuneOnlineEvalServings() {
    const { jobs, getServings } = useServingConfig()
    const chatStore = useChatStore()
    const [expand, setExpand] = useSetState({})

    const servingMap = useCreation(() => {
        return _.groupBy(getServings(), 'apiSpec.inference_type')
    }, [jobs])

    useUpdateEffect(() => {
        const llmchats = servingMap[InferenceType.llm_chat]
        if (!llmchats || !llmchats.length) {
            return
        }
        llmchats.forEach((serving) => chatStore.newSession(serving))
    }, [servingMap])

    return (
        <div className='serving-section flex flex-col flex-1 gap-20px overflow-auto'>
            {servingMap ? (
                Object.entries(servingMap).map(([key], index) => {
                    if (key === InferenceType.llm_chat) {
                        return (
                            <SectionAccordionPanel
                                key={key ?? index}
                                title={key}
                                expanded={expand[key] ?? true}
                                onExpanded={() => setExpand({ [key]: !expand[key] })}
                            >
                                <div className='serving-section px-20px transition-all'>
                                    <ChatGroup key={key} useChatStore={useChatStore} />
                                </div>
                            </SectionAccordionPanel>
                        )
                    }
                    return null
                })
            ) : (
                <BusyPlaceholder type='empty' />
            )}
            {Object.entries(servingMap).map(([key], index) => {
                if (key === InferenceType.llm_chat) {
                    return (
                        <SectionAccordionPanel
                            key={key ?? index}
                            title={key}
                            expanded={expand[key] ?? true}
                            onExpanded={() => setExpand({ [key]: !expand[key] })}
                        >
                            <div className='serving-section px-20px transition-all'>
                                <ChatGroup key={key} useChatStore={useChatStore} />
                            </div>
                        </SectionAccordionPanel>
                    )
                }
                return null
            })}
            {Object.entries(servingMap).map(([key], index) => {
                if (key === InferenceType.llm_chat) {
                    return (
                        <SectionAccordionPanel
                            key={key ?? index}
                            title={key}
                            expanded={expand[key] ?? true}
                            onExpanded={() => setExpand({ [key]: !expand[key] })}
                        >
                            <div className='serving-section px-20px transition-all'>
                                <ChatGroup key={key} useChatStore={useChatStore} />
                            </div>
                        </SectionAccordionPanel>
                    )
                }
                return null
            })}
        </div>
    )
}

import React from 'react'
import ChatGroup from '@starwhale/ui/Serving/components/ChatGroup'
import { InferenceType, useServingConfig } from '@starwhale/ui/Serving/store/config'
import { useChatStore } from '@starwhale/ui/Serving/store/chat'
import _ from 'lodash'
import { useUpdateEffect } from 'react-use'
import { useCreation, useSetState } from 'ahooks'
import SectionAccordionPanel from '@starwhale/ui/Serving/components/SectionAccordionPanel'
import { BusyPlaceholder } from '@starwhale/ui'
import WebGroup from '@starwhale/ui/Serving/components/WebGroup'

export default function FineTuneOnlineEvalServings() {
    const { jobs, getServings } = useServingConfig()
    const chatStore = useChatStore()
    const [expand, setExpand] = useSetState({})

    const servingMap = useCreation(() => {
        return _.groupBy(getServings(), 'type')
    }, [jobs])

    useUpdateEffect(() => {
        Object.entries(servingMap).forEach(([, list]) => {
            // chatStore.clearSessions()
            list.forEach((serving) => chatStore.newSession(serving))
        })
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
                                    <ChatGroup key={key} useStore={useChatStore} />
                                </div>
                            </SectionAccordionPanel>
                        )
                    }
                    if (key === InferenceType.web_handler) {
                        return (
                            <SectionAccordionPanel
                                key={key ?? index}
                                title={key}
                                expanded={expand[key] ?? true}
                                onExpanded={() => setExpand({ [key]: !expand[key] })}
                            >
                                <div className='serving-section px-20px transition-all'>
                                    <WebGroup key={key} useStore={useChatStore} />
                                </div>
                            </SectionAccordionPanel>
                        )
                    }
                    return null
                })
            ) : (
                <BusyPlaceholder type='empty' />
            )}
        </div>
    )
}

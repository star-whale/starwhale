import React, { useEffect } from 'react'
import ChatGroup from '@starwhale/ui/Serving/components/ChatGroup'
import { InferenceType, useServingConfig } from '@starwhale/ui/Serving/store/config'
import { useIfChanged } from '@starwhale/core'
import { useChatStore } from '@starwhale/ui/Serving/store/chat'
import _ from 'lodash'
import { useUpdateEffect } from 'react-use'
import { useCreation } from 'ahooks'

export default function FineTuneOnlineEvalServings() {
    const { jobs, getServings } = useServingConfig()
    const chatStore = useChatStore()

    const servingMap = useCreation(() => {
        return _.groupBy(getServings(), 'apiSpec.inference_type')
    }, [jobs])

    useUpdateEffect(() => {
        const llmchats = servingMap[InferenceType.llm_chat]
        if (!llmchats || !llmchats.length) {
            return
        }
        // chatStore.clearSessions()
        llmchats.forEach((serving) => chatStore.newSession(serving))
    }, [servingMap])

    return Object.entries(servingMap).map(([key]) => {
        if (key === InferenceType.llm_chat) {
            return <ChatGroup key={key} useChatStore={useChatStore} />
        }
        return null
    })
}

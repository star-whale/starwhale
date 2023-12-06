import React, { useEffect } from 'react'
import axios from 'axios'
import { useQuery } from 'react-query'
import { Select } from '@starwhale/ui'
import _ from 'lodash'
import { IApiSchema, InferenceType, ISpecSchema } from './schemas/api'
import { useChatStore } from '@starwhale/ui/Serving/store/chat'
import ChatGroup from './components/ChatGroup'

const fetchSpec = async () => {
    const { data } = await axios.get<ISpecSchema>('/api/spec')
    return data
}

export default function ServingPage() {
    const useFetchSpec = useQuery('spec', fetchSpec)
    const chatStore = useChatStore()

    const [spec, setSpec] = React.useState<ISpecSchema>()
    const [currentApi, setCurrentApi] = React.useState<IApiSchema>()

    useEffect(() => {
        if (!useFetchSpec.data) {
            return
        }
        const apiSpec = useFetchSpec.data.apis[0]
        chatStore.newOrUpdateSession({
            job: { id: 'client' } as any,
            type: apiSpec?.inference_type,
            exposedLink: {
                link: '',
                type: 'WEB_HANDLER',
                name: 'llm_chat',
            },
            apiSpec: useFetchSpec.data.apis[0],
        } as any)
        setSpec(useFetchSpec.data)
        setCurrentApi(useFetchSpec.data.apis[0])
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [useFetchSpec.data])

    return (
        <div>
            {(spec?.apis?.length ?? 0) > 1 && (
                <Select
                    options={_.map(spec?.apis, (i) => ({ id: i.inference_type, label: i.inference_type }))}
                    value={[{ id: currentApi?.inference_type, label: currentApi?.inference_type }]}
                    onChange={({ option }) => {
                        if (!option || !spec?.apis) {
                            return
                        }
                        const api = _.find(spec.apis, { inference_type: option.id })
                        if (!api) {
                            return
                        }
                        // @ts-ignore
                        setCurrentApi(api)
                    }}
                />
            )}
            {/* {currentApi?.inference_type === InferenceType.LLM_CHAT && <LLMChat api={currentApi} />} */}
            {currentApi?.inference_type === InferenceType.LLM_CHAT && <ChatGroup useStore={useChatStore} />}
        </div>
    )
}

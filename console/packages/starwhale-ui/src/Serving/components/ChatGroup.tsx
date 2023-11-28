import CasecadeResizer from '@starwhale/ui/AutoResizer/CascadeResizer'
import { useServingConfig } from '../store/config'
import ChatInput from './ChatInput'

function Chat({ data }) {
    return <div>{data?.id}</div>
}

function ChatGroup() {
    const { jobs } = useServingConfig()
    return (
        <div className='chat-group flex flex-col overflow-hidden'>
            <div className='overflow-x-auto'>
                1
                <CasecadeResizer>
                    {jobs.map((v, index) => (
                        <Chat key={v?.id ?? index} data={v} />
                    ))}
                </CasecadeResizer>
            </div>
            <ChatInput />
        </div>
    )
}

export default ChatGroup

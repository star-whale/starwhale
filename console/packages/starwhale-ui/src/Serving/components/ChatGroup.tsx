import ChatInput from './ChatInput'
import ChatList from './ChatList'

function ChatGroup() {
    return (
        <div className='chat-group flex flex-col overflow-hidden'>
            <div className='overflow-x-auto'>
                <ChatList />
            </div>
            <ChatInput />
        </div>
    )
}

export default ChatGroup

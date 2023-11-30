import React from 'react'
import CasecadeResizer from '@starwhale/ui/AutoResizer/CascadeResizer'
import { useChatStore as Store } from '../store/chat'
import { ExtendButton } from '@starwhale/ui/Button'
import JobStatus from '@/domain/job/components/JobStatus'
import { InferenceType } from '../store/config'

export const CHAT_PAGE_SIZE = 15
export const MAX_RENDER_MSG_COUNT = 45
type StoreT = typeof Store

function WebGroup({ useStore }: { useStore: StoreT }) {
    const store = useStore()

    return (
        <div className='web-group flex flex-col overflow-hidden'>
            <div className='flex overflow-x-auto gap-20px mb-10px text-nowrap flex-nowrap pb-10px'>
                <CasecadeResizer defaultConstraints={[400, 600]}>
                    {store.sessions
                        .filter((session) => session.show && session.serving.type === InferenceType.web_handler)
                        .map((session) => {
                            const { serving } = session
                            const { job } = serving

                            return (
                                <div
                                    key={session.id}
                                    className='web rounded-4px border-1 border-[#cfd7e6] h-full overflow-hidden flex flex-col pb-15px bg-white'
                                >
                                    <div className='web-title flex lh-none h-40px bg-[#eef1f6] px-10px items-center'>
                                        <ExtendButton
                                            disabled={!session?.serving}
                                            icon={session?.show ? 'eye' : 'eye_off'}
                                            styleas={[
                                                'menuoption',
                                                'nopadding',
                                                'iconnormal',
                                                !session?.serving ? 'icondisable' : undefined,
                                            ]}
                                            onClick={() => store.onSessionShowById(job.id, !session?.show)}
                                        />
                                        <div className='flex-1 mx-8px font-600'>{job?.modelName}</div>
                                        <div>
                                            <JobStatus status={job?.jobStatus as any} />
                                        </div>
                                    </div>

                                    {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
                                    <div className='web-body flex-1 overflow-hidden p-10px pb-0px relative overscroll-none flex gap-20px flex-col min-w-0 h-full bg-white'>
                                        <iframe
                                            title='web-handler'
                                            src={`${serving.exposedLink?.link}?__theme=light`}
                                            style={{
                                                position: 'absolute',
                                                height: '100%',
                                                width: '100%',
                                                border: 'none',
                                            }}
                                        />
                                    </div>
                                </div>
                            )
                        })}
                </CasecadeResizer>
            </div>
        </div>
    )
}

export default WebGroup

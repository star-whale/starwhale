import { useFetchReportPreview } from '@/domain/report/hooks/useReport'
import User from '@/domain/user/components/User'
import React from 'react'
import { formatTimestampDateTime } from '@/utils/datetime'
import { IconFont } from '@starwhale/ui'
import TiptapEditor from '@starwhale/ui/TiptapEditor'
import Avatar from '@/components/Avatar'
import { useQueryArgs } from '@/hooks/useQueryArgs'

export default function ReportPreview() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { query } = useQueryArgs()
    const { rid } = query || {}
    const info = useFetchReportPreview(rid)
    const { data } = info

    return (
        <div className='flex flex-column flex-1'>
            <h1
                className='mb-20px'
                style={{
                    fontSize: '34px',
                    fontWeight: 600,
                    color: '#02102B',
                }}
            >
                {data?.title || ''}
            </h1>
            <div className='flex gap-10px items-center font-14px'>
                <div className='flex gap-5px'>
                    <Avatar name={info.data?.owner?.name} size={28} />
                    <User user={info.data?.owner} />|{' '}
                </div>
                <span style={{ color: 'rgba(2,16,43,0.60)' }}>
                    <IconFont type='time' style={{ marginRight: '4px' }} />
                    {formatTimestampDateTime(data?.createdTime || Date.now())}
                </span>
            </div>
            <p className='mt-20px' style={{ fontSize: '16px', color: '#02102B' }}>
                {data?.description || ''}
            </p>
            <div className='mb-20px flex-1'>
                <TiptapEditor id={rid} initialContent={data?.content} editable={false} />
            </div>
        </div>
    )
}

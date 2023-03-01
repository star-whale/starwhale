import React from 'react'
import { IOnlineEvalStatusSchema } from '@project/schemas/OnlineEval'
import { formatTimestampDateTime } from '@/utils/datetime'
import { LabelSmall } from 'baseui/typography'

export default function OnlineEvalLoading({ events }: IOnlineEvalStatusSchema) {
    return (
        <div>
            <div style={{ textAlign: 'center' }}>
                <LabelSmall style={{ color: '#2B65D9', marginTop: '4px' }}>
                    The process is loaded and running
                </LabelSmall>
                <LabelSmall style={{ color: '#2B65D9', marginTop: '4px' }}>wait for a moment please</LabelSmall>
            </div>
            <div
                className='progress progress-striped active'
                style={{
                    marginTop: '16px',
                    marginBottom: '30px',
                    marginLeft: '0px',
                    marginRight: '0px',
                }}
            >
                <div
                    style={{
                        width: '100%',
                        borderRadius: '5px',
                    }}
                    className='progress-bar'
                >
                    <span>&nbsp;</span>
                </div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                {events?.map((e, i) => (
                    <p key={i} style={{ textAlign: 'left' }}>
                        <LabelSmall>{formatTimestampDateTime(e.eventTimeInMs, 'YYYY-MM-DD HH:mm:ss')}:</LabelSmall>
                        <LabelSmall style={{ color: ' rgba(2,16,43,0.60)', marginTop: '4px' }}>{e.message}</LabelSmall>
                    </p>
                ))}
            </div>
        </div>
    )
}

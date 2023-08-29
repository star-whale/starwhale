import React from 'react'
import { IOnlineEvalStatusSchema } from '@project/schemas/OnlineEval'
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
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>{events}</div>
        </div>
    )
}

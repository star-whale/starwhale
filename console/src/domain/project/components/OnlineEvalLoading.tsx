import React from 'react'
import { ProgressBar } from 'baseui/progress-bar'
import { IOnlineEvalStatusSchema } from '@project/schemas/OnlineEval'
import { formatTimestampDateTime } from '@/utils/datetime'

export default function OnlineEvalLoading({ progress, events }: IOnlineEvalStatusSchema) {
    return (
        <ProgressBar
            value={progress}
            infinite
            showLabel
            overrides={{
                Label: {
                    style: ({ $theme }) => ({
                        color: $theme.colors.primary,
                    }),
                },
            }}
            getProgressLabel={() => {
                return (
                    <div>
                        {events?.map((e) => (
                            <p key={e.eventTimeInMs} style={{ textAlign: 'left' }}>
                                {formatTimestampDateTime(e.eventTimeInMs, 'YYYY-MM-DD HH:mm:ss')}: {e.message}
                            </p>
                        ))}
                    </div>
                )
            }}
        />
    )
}

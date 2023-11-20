import React from 'react'
import { api } from '@/api'
import { useParams } from 'react-router-dom'
import useFineTuneColumns, { OVERVIEW_COLUMNS_KEYS } from '@/domain/space/hooks/useFineTuneColumns'

export default function FineTuneOverview() {
    const { projectId, spaceId, fineTuneId } = useParams<{ projectId: any; spaceId: any; fineTuneId; any }>()
    const info = api.useFineTuneInfo(projectId, spaceId, fineTuneId)
    const { renderCell, columns } = useFineTuneColumns({
        keys: OVERVIEW_COLUMNS_KEYS,
    })
    const renderer = renderCell(info.data)

    return (
        <div className='flex-column overflow-auto'>
            {columns.map((v) => (
                <div
                    key={v?.key}
                    style={{
                        display: 'flex',
                        gap: '20px',
                        borderBottom: '1px solid #EEF1F6',
                        lineHeight: '44px',
                        flexWrap: 'nowrap',
                        fontSize: '14px',
                        paddingLeft: '12px',
                    }}
                >
                    <div className='basis-170px overflow-hidden text-ellipsis flex-shrink-0 color-[rgba(2,16,43,0.60)]'>
                        {v?.title}
                    </div>
                    <div className='py-13px lh-18px'>{renderer(v.key)}</div>
                </div>
            ))}
        </div>
    )
}

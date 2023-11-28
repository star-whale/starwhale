import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { IJobVo, IPageInfoFineTuneVo } from '@/api'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'
import { useEventCallback } from '@starwhale/core'
import { useCreation } from 'ahooks'

function FineTuneCard({ ft, onClick, viewId }) {
    const [t] = useTranslation()
    const { renderCell } = useFineTuneColumns()
    const renderer = useEventCallback(renderCell(ft))
    const isFocus = viewId === String(ft.id)
    const ref = React.useRef<HTMLDivElement>(null)

    React.useEffect(() => {
        if (isFocus) {
            ref.current?.scrollIntoView({
                behavior: 'smooth',
                block: 'center',
                inline: 'center',
            })
        }
    }, [isFocus, ft.id])

    return (
        <div
            ref={ref}
            className='flex px-16px py-16px border-1 rounded-sm h-110px gap-10px lh-none overflow-hidden flex-shrink-0'
            onClick={onClick}
            role='button'
            tabIndex={0}
            style={{
                border: viewId === String(ft.id) ? '1px solid #2B65D9' : '1px solid #CFD7E6',
            }}
        >
            <div className='flex-1 flex flex-col justify-between'>
                <div className='flex justify-between items-center font-600'>
                    {renderer('baseModelName')}
                    {renderer('status')}
                </div>
                <div className='flex-1 items-center mt-12px mb-auto'>{renderer('baseModelVersionAlias')}</div>
                <div className='flex justify-between items-center color-[rgba(2,16,43,0.60)]'>
                    {renderer('createdTime')}
                    <p className='flex-shrink-0 flex gap-3px'>
                        {t('Created by')} {renderer('owner')}
                    </p>
                </div>
            </div>
        </div>
    )
}

export default function FineTuneOnlineEvalJobCard({
    list,
    viewId,
    onView,
}: {
    list?: IJobVo
    onView?: (id: number) => void
    viewId?: any
}) {
    const cards = useCreation(() => {
        return list?.map((ft) => <FineTuneCard key={ft.id} ft={ft} onClick={() => onView?.(ft.id)} viewId={viewId} />)
    }, [list, viewId])

    return <div className='ft-table-card content-full-scroll gap-10px pr-5px'>{cards}</div>
}

import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { IPageInfoFineTuneVo } from '@/api'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'
import Checkbox from '@starwhale/ui/Checkbox'

function FineTuneCard({ ft, onClick, viewId }) {
    const [t] = useTranslation()
    const { renderCell } = useFineTuneColumns()
    const renderer = renderCell(ft)

    return (
        <div
            className='flex px-10px py-16px border-1 rounded-sm h-110px gap-10px lh-none overflow-hidden flex-shrink-0'
            onClick={onClick}
            role='button'
            tabIndex={0}
            style={{
                border: viewId === ft.id ? '1px solid #2B65D9' : '1px solid #CFD7E6',
            }}
        >
            <div className='mr-8px'>
                <Checkbox />
            </div>
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

export default function FineTuneRunsTableCard({
    data,
    viewId,
    onView,
}: {
    data?: IPageInfoFineTuneVo
    onView?: (id: number) => void
    viewId?: any
}) {
    return (
        <div className='ft-table-card content-full-scroll gap-10px pr-5px'>
            {data?.list?.map((ft) => {
                return <FineTuneCard key={ft.id} ft={ft} onClick={() => onView?.(ft.id)} viewId={viewId} />
            })}
        </div>
    )
}

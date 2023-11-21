import React from 'react'

const KIND = {
    accent: 'bg-[#ededff] text-[#4848B3] ',
    positive: 'bg-[#e6fff4] text-[#00b368]',
}

function StatusTag({ children, kind = 'accent' }: { children: React.ReactNode; kind?: keyof typeof KIND }) {
    return <span className={`py-3px px-10px text-12px lh-none rounded-12px ${KIND[kind]}`}>{children}</span>
}
export { StatusTag, KIND }
export default StatusTag

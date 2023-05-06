import React from 'react'

let HeaderExtend: React.FC = () => <></>

export function registerExtensions(components: { HeaderExtend: React.FC }) {
    if (!components) return
    HeaderExtend = components?.HeaderExtend
}

export function HeaderExtends() {
    return <HeaderExtend />
}

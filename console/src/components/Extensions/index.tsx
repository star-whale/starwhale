import React from 'react'
import FormFieldResource from '@/domain/job/components/FormFieldResource'

let HeaderExtendTmp: React.FC = () => <></>
let FormFieldResourceTmp = FormFieldResource
let FormFieldPriTmp: React.FC = () => <></>

export function registerExtensions(components: any) {
    if (!components) return
    HeaderExtendTmp = components?.HeaderExtend
    FormFieldResourceTmp = components?.FormFieldResource
    FormFieldPriTmp = components?.FormFieldPri
}

export function HeaderExtends() {
    return <HeaderExtendTmp />
}

export function FormFieldResourceExtend(props: any) {
    if (!FormFieldResourceTmp) return null
    return <FormFieldResourceTmp {...props} />
}

export function FormFieldPriExtend(props: any) {
    if (!FormFieldPriTmp) return null
    return <FormFieldPriTmp {...props} />
}

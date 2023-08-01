import React from 'react'
import FormFieldResource from '@/domain/job/components/FormFieldResource'
import FormFieldAutoRelease from '@/domain/job/components/FormFieldAutoRelease'

let HeaderExtendTmp: React.FC = () => <></>
let FormFieldResourceTmp = FormFieldResource
let FormFieldPriTmp: React.FC = () => <></>
let FormFieldAutoReleaseTmp = FormFieldAutoRelease

export function registerExtensions(components: any) {
    if (!components) return
    HeaderExtendTmp = components?.HeaderExtend
    FormFieldResourceTmp = components?.FormFieldResource
    FormFieldPriTmp = components?.FormFieldPri
    FormFieldAutoReleaseTmp = components?.FormFieldAutoRelease
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

export function FormFieldAutoReleaseExtend(props: any) {
    if (!FormFieldAutoReleaseTmp) return null
    return <FormFieldAutoReleaseTmp {...props} />
}

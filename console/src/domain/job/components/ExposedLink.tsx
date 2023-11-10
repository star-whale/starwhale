import React from 'react'
import { ExposedLinkType } from '@job/schemas/job'
import IconFont, { IconTypesT } from '@starwhale/ui/IconFont'
import Link from '@/components/Link/Link'
import { ExtendButton, IExtendButtonProps } from '@starwhale/ui'
import { IExposedLinkVo } from '@/api'

export interface IExposedLinkProps {
    data: IExposedLinkVo
}

const VSCODE_NAME = 'VS_CODE'

function ExposedLink({ data: { type, name, link } }: IExposedLinkProps) {
    let font: IconTypesT = 'global'
    let title = name
    if (type === ExposedLinkType.DEV_MODE && name === VSCODE_NAME) {
        font = 'vscode'
        title = 'vscode'
    }

    return (
        <Link
            rel='noreferrer'
            content={title}
            tooltip={{
                content: title,
            }}
            target='_blank'
            to={link}
            style={{ color: 'rgb(43, 101, 217)' }}
        >
            <IconFont type={font} size={16} />
        </Link>
    )
}

function ExposedButtonLink({
    data: { type, name, link },
    hasText = false,
    ...rest
}: IExposedLinkProps & IExtendButtonProps & { hasText?: boolean }) {
    let font: IconTypesT = 'global'
    let title = name
    if (type === ExposedLinkType.DEV_MODE && name === VSCODE_NAME) {
        font = 'vscode'
        title = 'vscode'
    }

    return (
        <Link
            rel='noreferrer'
            content={title}
            tooltip={{
                content: title,
            }}
            target='_blank'
            to={link}
            style={{ color: 'rgb(43, 101, 217)' }}
        >
            <ExtendButton {...rest} icon={font} onClick={() => {}}>
                {hasText ? title : undefined}
            </ExtendButton>
        </Link>
    )
}
export { ExposedButtonLink }
export default ExposedLink

import React from 'react'
import { ExposedLinkType, IExposedLinkSchema } from '@job/schemas/job'
import IconFont, { IconTypesT } from '@starwhale/ui/IconFont'

export interface IExposedLinkProps {
    data: IExposedLinkSchema
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
        <a key={link} target='_blank' href={link} rel='noreferrer' title={title}>
            <IconFont type={font} size={16} />
        </a>
    )
}

export default ExposedLink

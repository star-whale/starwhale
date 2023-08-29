import React from 'react'
import { ExposedLinkType, IExposedLinkSchema } from '@job/schemas/job'
import IconFont, { IconTypesT } from '@starwhale/ui/IconFont'
import Link from '@/components/Link/Link'

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

export default ExposedLink

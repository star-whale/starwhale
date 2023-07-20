import React from 'react'
import { getUploadName, getUploadType } from './utils'
import IconFont from '@starwhale/ui/IconFont'
import { getReadableStorageQuantityStr } from '@starwhale/ui/utils'
import Button, { IButtonProps } from '@starwhale/ui/Button'
import { UploadFile } from './types'
import { ProgressBar } from 'baseui/progress-bar'
import { expandMargin } from '@/utils'

const overrides: IButtonProps['overrides'] = {
    BaseButton: {
        style: {
            'marginLeft': '16px',
            'backgroundColor': 'transparent',
            'color': ' rgba(2,16,43,0.40);',
            ':hover .icon-container': {
                color: '#D65E5E !important',
                backgroundColor: 'transparent',
            },
            ':focus': {
                color: '#D65E5E !important',
                backgroundColor: 'transparent',
            },
        },
    },
}

export function ItemRender({
    percent = -1,
    file,
    onRemove,
}: {
    percent?: number
    file: UploadFile
    onRemove?: (file: UploadFile) => void
}) {
    // return originNode
    const type = getUploadType(file)
    const name = getUploadName(file)
    const icons = {
        IMAGE: 'image',
        VIDEO: 'video',
        AUDIO: 'audio',
        CSV: 'txt',
        JSON: 'txt',
        JSONL: 'txt',
    }

    return (
        <div className='ant-upload-list-item-container'>
            <div className='ant-upload-list-item'>
                <div className='ant-upload-icon'>
                    {/* @ts-ignore */}
                    <IconFont type={icons[type] ?? 'file2'} />
                </div>
                <div className='ant-upload-list-item-name'>{name}</div>
                <div className='ant-upload-list-item-size'>{getReadableStorageQuantityStr(file.size ?? 0)}</div>
                <div className='ant-upload-list-item-actions'>
                    <Button
                        as='link'
                        icon='delete'
                        onClick={() => {
                            onRemove?.(file)
                        }}
                        overrides={overrides}
                    />
                </div>
            </div>
            {percent >= 0 && (
                <ProgressBar
                    value={percent}
                    overrides={{
                        BarContainer: {
                            style: {
                                ...expandMargin('0px', '0px', '0px', '0px'),
                            },
                        },
                        BarProgress: {
                            style: {
                                height: '1px',
                            },
                        },
                        Bar: {
                            style: {
                                height: '1px',
                            },
                        },
                    }}
                />
            )}
        </div>
    )
}

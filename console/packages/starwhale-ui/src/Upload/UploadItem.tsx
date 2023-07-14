import React from 'react'
import { getUploadName, getUploadType } from './utils'
import IconFont from '@starwhale/ui/IconFont'
import { getReadableStorageQuantityStr } from '@starwhale/ui/utils'
import Button from '@starwhale/ui/Button'
import { UploadFile } from './types'

export function ItemRender({ file, onRemove }: { file: UploadFile; onRemove?: (file: UploadFile) => void }) {
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
                        onClick={() => onRemove?.(file)}
                        overrides={{
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
                        }}
                    />
                </div>
            </div>
        </div>
    )
}

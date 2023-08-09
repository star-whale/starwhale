import React, { useEffect } from 'react'
import { IHasTagSchema } from '@base/schemas/resource'
import { Tag } from 'baseui/tag'
import { Input, useConfirmCtx } from '@starwhale/ui'
import { mergeOverrides } from '@starwhale/ui/base/helpers/overrides'
import { Delete, Plus } from 'baseui/icon'
import useTranslation from '@/hooks/useTranslation'

export function Alias({ alias = '' }: { alias?: string }) {
    return (
        <div style={{ display: 'inline-flex', gap: '2px', alignItems: 'center' }}>
            {alias.split(',').map((item, index) => {
                return (
                    <span
                        key={index}
                        style={{
                            fontSize: '14px',
                            lineHeight: '14px',
                            borderRadius: '4px',
                            backgroundColor: '#F5F8FF',
                            border: '1px solid #CFD7E6 ',
                            padding: '2px 8px',
                            color: '#02102B',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {item}
                    </span>
                )
            })}
        </div>
    )
}

const tagOverrides = {
    Root: {
        style: {
            height: '20px',
            borderRadius: '4px',
            border: '1px #CFD7E6',
            backgroundColor: '#F5F8FF',
            padding: '2px 8px',
            color: '#02102B',
            fontWeight: 'inherit',
            borderStyle: 'solid',
            marginLeft: '2px',
            marginRight: '2px',
        },
    },
    Text: {
        style: {
            fontSize: '14px',
            maxWidth: null,
        },
    },
}

export interface IEditableAliasProps {
    resource: IHasTagSchema
    onAddTag: (tag: string) => void
    onRemoveTag: (tag: string) => void
}

export function EditableAlias({ resource, onAddTag, onRemoveTag }: IEditableAliasProps) {
    const tags = resource.tags ?? []

    const newTagOverrides = mergeOverrides(tagOverrides, {
        Root: {
            style: {
                borderStyle: 'dashed',
                backgroundColor: 'inherit',
            },
        },
    })

    const confirmCtx = useConfirmCtx()
    const [t] = useTranslation()
    const inputRef = React.useRef<HTMLInputElement>(null)
    const [showInput, setShowInput] = React.useState(false)

    useEffect(() => {
        if (showInput) {
            inputRef.current?.focus()
        }
    }, [showInput])

    const handleAddTag = (tag: string) => {
        if (tag) {
            onAddTag(tag)
        }
        setShowInput(false)
    }

    const constTag = (title: string) => {
        return title ? (
            <Tag overrides={tagOverrides} closeable={false}>
                {title}
            </Tag>
        ) : null
    }

    const userTag = (title: string, idx: number) => {
        return title ? (
            <Tag key={idx} overrides={tagOverrides} closeable={false}>
                {/* we draw the delete icon by ourselves, because the built-in one is too big and the viewbox can not change
                https://github.com/uber/baseweb/blob/b452864a5b2fcdf9867731b07192e3b1fab3501a/src/tag/tag.tsx#L26 */}
                <div style={{ display: 'flex' }}>
                    <span style={{ marginRight: '2px', marginTop: '1.5px' }}>{title}</span>
                    <Delete
                        onClick={async () => {
                            const ok = await confirmCtx.show({
                                title: t('Confirm'),
                                content: t('delete sth confirm', [title]),
                            })
                            if (ok) {
                                onRemoveTag(title)
                            }
                        }}
                        cursor='pointer'
                    />
                </div>
            </Tag>
        ) : null
    }

    return (
        <div style={{ display: 'flex', flexFlow: 'row wrap', alignItems: 'stretch' }}>
            {constTag(resource.alias)}
            {constTag(resource.latest ? 'latest' : '')}
            {tags.map(userTag)}
            {showInput ? (
                <div style={{ width: '100px' }}>
                    <Input
                        size='mini'
                        overrides={{
                            Root: {
                                style: {
                                    height: '20px',
                                    marginTop: '5px',
                                },
                            },
                            Input: {
                                style: {
                                    paddingLeft: '4px',
                                    paddingRight: '4px',
                                },
                            },
                        }}
                        inputRef={inputRef}
                        onKeyPress={(e) => {
                            if (e.key === 'Enter') {
                                handleAddTag(e.currentTarget.value)
                            }
                        }}
                        onBlur={() => {
                            setShowInput(false)
                        }}
                    />
                </div>
            ) : (
                <Tag
                    overrides={newTagOverrides}
                    closeable={false}
                    startEnhancer={() => <Plus size={16} />}
                    onClick={() => {
                        setShowInput(true)
                    }}
                >
                    New
                </Tag>
            )}
        </div>
    )
}

export default Alias

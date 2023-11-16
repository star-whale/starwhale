import React from 'react'
import { ExtendButton } from '@starwhale/ui'
import { useProject } from '@/domain/project/hooks/useProject'
import { TreeNodeData } from '@starwhale/ui/base/tree-view/types'
import { useMount } from 'ahooks'

export enum QuickGroupEnum {
    latest = 'latest',
    current = 'current',
    guest = 'guest',
    all = 'all',
}

function QuickGroup({
    filters = [],
    value = QuickGroupEnum.current,
    options = [],
    onChange,
}: {
    filters: any[]
    value: QuickGroupEnum
    options: { key: string; label: string }[]
    onChange: (key: string, filters: any[]) => void
}) {
    // const [quickGroup, setQuickGroup] = useState<QuickGroupEnum>(QuickGroupEnum.all)
    const { project } = useProject()
    const filterFunctions = {
        [QuickGroupEnum.latest]: [() => false],
        [QuickGroupEnum.current]: [(item: TreeNodeData) => item?.info?.data?.projectName === project?.name],
        [QuickGroupEnum.guest]: [(item: TreeNodeData) => item?.info?.data?.projectName !== project?.name],
        [QuickGroupEnum.all]: [() => true],
        ...filters,
    }
    const quickGroup = value

    useMount(() => {
        onChange?.(QuickGroupEnum.current, filterFunctions[QuickGroupEnum.current])
    })

    return (
        <div>
            <div key='action' className='flex items-center gap-4px mb-4px mt-12px'>
                {options?.map((item) => {
                    return (
                        <ExtendButton
                            key={item.key}
                            kind='tertiary'
                            type='button'
                            onClick={() => {
                                // setQuickGroup(item.key as QuickGroupEnum)
                                onChange?.(item.key, filterFunctions[item.key])
                            }}
                            styleas={['transparent']}
                            overrides={{
                                BaseButton: {
                                    style: {
                                        'paddingLeft': '8px',
                                        'paddingRight': '8px',
                                        'color': quickGroup === item.key ? '#2B65D9;' : 'rgba(2,16,43,0.60)',
                                        'backgroundColor': quickGroup === item.key ? ' #EBF1FF;' : 'transparent',
                                        ':hover': {
                                            backgroundColor: '#EBF1FF',
                                        },
                                        ':active': {
                                            backgroundColor: '#EBF1FF',
                                        },
                                        ':focus': {
                                            backgroundColor: '#EBF1FF',
                                        },
                                    },
                                },
                            }}
                        >
                            {item.label}
                        </ExtendButton>
                    )
                })}
            </div>
        </div>
    )
}

export { QuickGroup }
export default QuickGroup

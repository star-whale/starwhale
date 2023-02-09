import React from 'react'
import { useStyletron } from 'baseui'
import { LabelSmall } from 'baseui/typography'
import { SORT_DIRECTIONS } from '@starwhale/ui/base/data-table/constants'
import { RadioGroup, Radio, ALIGN } from '../../Radio'
import { ColumnT, ConfigT } from './types'
import ConfigManageColumns from './config-manage-columns'
import Input from '../../Input'
import Select from '../../Select'
import ConfigQuery from './config-query'

type ViewListPropsT = {
    view: ConfigT
    columns: ColumnT[]
    rows: any[]
}
function ViewEdit(props: ViewListPropsT, ref: React.Ref<any>) {
    const [css] = useStyletron()
    // const [t] = useTranslation()
    const [name, setName] = React.useState(props.view?.name ?? '')
    const [sortBy, setSortBy] = React.useState(props.view?.sortBy ?? '')
    const [queries, setQueries] = React.useState(props.view?.queries ?? [])
    const $keys = React.useMemo(() => {
        return props.columns.map((column) => {
            return {
                id: column.key,
                label: column.title,
            }
        })
    }, [props.columns])
    const [sortDirection, setSortDirection] = React.useState(props.view?.sortDirection ?? SORT_DIRECTIONS.ASC)

    const filterRef = React.useRef(null)
    const columnRef = React.useRef(null)

    React.useImperativeHandle(
        ref,
        () => ({
            getView: () => {
                return {
                    ...props.view,
                    ...(columnRef.current as any).getConfig(),
                    // filters: (filterRef.current as any).getCategories(),
                    name,
                    queries,
                    sortBy,
                    sortDirection,
                }
            },
        }),
        [name, filterRef, columnRef, sortBy, sortDirection, props.view, queries]
    )

    return (
        <>
            <div
                className={css({
                    width: '280px',
                    display: 'flex',
                    gap: '10px',
                    flexDirection: 'column',
                })}
            >
                <LabelSmall>View Name *</LabelSmall>
                <Input
                    placeholder='input text'
                    required
                    clearable
                    value={name}
                    onChange={(event) => setName((event.target as HTMLInputElement).value)}
                />
            </div>
            {/* <CategoricalFilter
                ref={filterRef}
                isInline
                columns={props.columns}
                rows={props.rows}
                filters={props.view?.filters ?? []}
            /> */}
            <div
                className={css({
                    display: 'flex',
                    gap: '10px',
                    flexDirection: 'column',
                })}
            >
                <LabelSmall>Add Filter</LabelSmall>
                <ConfigQuery value={queries} columns={props.columns} onChange={setQueries} />
            </div>
            <div className='inherit-height' style={{ minHeight: '300px' }}>
                <ConfigManageColumns ref={columnRef} isInline view={props.view} columns={props.columns ?? []} />
            </div>
            <div
                className={css({
                    display: 'flex',
                    gap: '10px',
                    flexDirection: 'column',
                })}
            >
                <LabelSmall>Sort</LabelSmall>
                <div
                    style={{
                        display: 'grid',
                        gap: '20px',
                        gridTemplateColumns: '280px 1fr',
                    }}
                >
                    <Select
                        size='compact'
                        options={$keys}
                        placeholder='select...'
                        clearable={false}
                        onChange={(params: any) => {
                            if (!params.option) {
                                return
                            }
                            setSortBy?.(params.option?.id as string)
                        }}
                        value={sortBy ? [{ id: sortBy }] : []}
                    />
                    <RadioGroup
                        value={sortDirection}
                        // @ts-ignore
                        onChange={(e) => setSortDirection(e.currentTarget.value)}
                        name='number'
                        align={ALIGN.horizontal}
                    >
                        <Radio value='ASC'>Ascending</Radio>
                        <Radio value='DESC'>Descending</Radio>
                    </RadioGroup>
                </div>
            </div>
        </>
    )
}

export default React.forwardRef(ViewEdit)

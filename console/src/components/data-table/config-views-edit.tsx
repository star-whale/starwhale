import * as React from 'react'
import { useStyletron } from 'baseui'
import { LabelSmall } from 'baseui/typography'
import { ColumnT, ConfigT } from './types'
import ConfigManageColumns from './config-manage-columns'
import Input from '../Input'
import { CategoricalFilter } from './filter-operate-menu'

type ViewListPropsT = {
    view: ConfigT
    columns: ColumnT[]
    rows: any[]
}
function ViewEdit(props: ViewListPropsT, ref: React.Ref<any>) {
    const [css] = useStyletron()
    // const [t] = useTranslation()
    const [name, setName] = React.useState(props.view?.name ?? '')

    const filterRef = React.useRef(null)
    const columnRef = React.useRef(null)

    React.useImperativeHandle(
        ref,
        () => ({
            getView: () => ({
                ...props.view,
                name,
                filters: (filterRef.current as any).getCategories(),
                ...(columnRef.current as any).getConfig(),
            }),
        }),
        [name, filterRef, columnRef, props.view]
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
                <Input required value={name} onChange={(event) => setName((event.target as HTMLInputElement).value)} />
            </div>
            <CategoricalFilter
                ref={filterRef}
                isInline
                columns={props.columns}
                rows={props.rows}
                filters={props.view?.filters ?? []}
            />
            <ConfigManageColumns ref={columnRef} isInline view={props.view} columns={props.columns ?? []} />
        </>
    )
}

export default React.forwardRef(ViewEdit)

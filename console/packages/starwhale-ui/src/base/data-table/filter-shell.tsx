// @flow

import * as React from 'react'

import { Checkbox, STYLE_TYPE } from 'baseui/checkbox'
import { useStyletron } from 'baseui'
import { KIND, SIZE } from 'baseui/button'
import { LocaleContext } from 'baseui/locale'
import Button from '../../Button'

export type ExcludeKind = 'value' | 'range'

type PropsT = {
    children: React.ReactNode
    hasExclude?: boolean
    exclude: boolean
    excludeKind?: ExcludeKind
    onExcludeChange: () => void
    onApply: () => void
    onSave?: () => void
    onSaveAs?: () => void
}

function FilterShell(props: PropsT) {
    const [css, theme] = useStyletron()
    const locale = React.useContext(LocaleContext)
    const { hasExclude = false } = props
    let excludeText
    switch (props.excludeKind) {
        case 'value':
            excludeText = locale.datatable.filterExcludeValue
            break
        case 'range':
            excludeText = locale.datatable.filterExcludeRange
            break
        default:
            excludeText = locale.datatable.filterExclude
    }
    return (
        <form
            className={css({
                backgroundColor: theme.colors.backgroundPrimary,
                paddingTop: '28px',
                paddingRight: '28px',
                paddingBottom: '28px',
                paddingLeft: '28px',
                width: '655px',
                // width: FILTER_SHELL_WIDTH,
            })}
            onSubmit={(event) => {
                event.preventDefault()
                props.onApply()
            }}
        >
            {props.children}
            <div
                className={css({
                    display: 'flex',
                    justifyContent: 'flex-start',
                    alignItems: 'flex-end',
                    marginTop: theme.sizing.scale600,
                    gap: theme.sizing.scale200,
                })}
            >
                {hasExclude && (
                    <div
                        className={css({
                            alignSelf: 'flex-start',
                        })}
                    >
                        <Checkbox
                            checked={props.exclude}
                            onChange={props.onExcludeChange}
                            checkmarkType={STYLE_TYPE.toggle_round}
                            labelPlacement='right'
                        >
                            {excludeText}
                        </Checkbox>
                    </div>
                )}
                {props.onSaveAs && (
                    <Button onClick={props.onSaveAs} kind={KIND.secondary} size={SIZE.mini}>
                        Save AS
                    </Button>
                )}
                {props.onSave && (
                    <Button onClick={props.onSave} kind={KIND.secondary} size={SIZE.mini}>
                        Save
                    </Button>
                )}
                <Button
                    size={SIZE.mini}
                    type='submit'
                    overrides={{
                        BaseButton: {
                            style: {
                                marginLeft: 'auto',
                            },
                        },
                    }}
                >
                    {locale.datatable.filterApply}
                </Button>
            </div>
        </form>
    )
}

export default FilterShell

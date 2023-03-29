import { LabelSmall } from 'baseui/typography'
import IconFont from '../IconFont'
import { themedStyled } from '../theme/styletron'

export const SelectorContainer = themedStyled('div', ({ $isEditing, $isGrid }) => {
    const $style = $isGrid
        ? {
              maxHeight: '128px',
              height: 'auto',
              minHeight: '32px',
              alignItems: 'flex-start',
              overflowX: 'auto',
              overflowY: 'auto',
          }
        : {
              height: '32px',
              alignItems: 'center',
              overflowX: 'auto',
              overflowY: 'hidden',
          }

    return {
        'display': 'flex',
        'lineHeight': '20px',
        'padding': '4px',
        'borderRadius': '4px',
        '&::-webkit-scrollbar': {
            height: '4px !important',
        },
        'flexGrow': '0',
        '&:hover': {
            borderColor: '#799EE8 !important',
        },
        'borderWidth': '1px',
        'borderColor': $isEditing ? '#799EE8' : '#CFD7E6',
        ...$style,
    }
})
SelectorContainer.displayName = 'SelectorContainer'

export const SelectorItemsContainer = themedStyled('div', ({ $isGrid }) => ({
    display: 'flex',
    flexGrow: '1',
    gap: '10px',
    height: $isGrid ? 'auto' : '30px',
    lineHeight: '20px',
    alignItems: 'center',
}))
SelectorItemsContainer.displayName = 'SelectorItemsContainer'

export const StartEnhancer = themedStyled('div', () => ({
    width: '34px',
    height: '22px',
    display: 'grid',
    placeItems: 'center',
    flexShrink: 0,
}))
StartEnhancer.displayName = 'StartEnhancer'

export const Placeholder = themedStyled('div', () => ({
    position: 'relative',
    display: 'flex',
    width: 0,
    alignItems: 'center',
}))
Placeholder.displayName = 'Placeholder'

export const SelectItemContainer = themedStyled('div', ({ $isGrid }) => {
    const $style = $isGrid
        ? {
              width: '100%',
          }
        : {
              display: 'flex',
              flexWrap: 'nowrap',
              width: 'auto',
              height: '22px',
          }

    return {
        'position': 'relative',
        'gap': '1px',
        'cursor': 'pointer',
        'lineHeight': '22px',
        '&:hover .label': {
            backgroundColor: '#EDF3FF',
        },
        ...$style,
    }
})
SelectItemContainer.displayName = 'SelectItemContainer'

export const LabelsContainer = themedStyled('div', ({ $multiple }) => ({
    display: 'Grid',
    gridTemplateColumns: !$multiple ? '1fr' : 'repeat(2, 1fr)',
    gap: '1px',
}))
LabelsContainer.displayName = 'LabelsContainer'

export const LabelContainer = themedStyled('div', () => ({
    height: '24px',
    lineHeight: '24px',
    padding: '0 8px',
    background: '#EEF1F6',
    borderRadius: '4px',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    overflow: ' hidden',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
}))
LabelContainer.displayName = 'LabelContainer'

export const LabelRemove = themedStyled('div', () => ({
    height: '22px',
    lineHeight: '22px',
    padding: '0 8px',
    background: '#EEF1F6',
    borderRadius: '4px',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    overflow: ' hidden',
    display: 'flex',
    alignItems: 'center',
}))
LabelRemove.displayName = 'LabelRemove'

export const AutosizeInputContainer = themedStyled('div', ({ $isEditing }) => ({
    minWidth: $isEditing ? '100px' : 0,
    display: 'inline-block',
    maxWidth: '100%',
    position: 'relative',
    // flex: 1,
    flexBasis: $isEditing ? '100px' : 0,
    width: $isEditing ? '100%' : 0,
    height: '100%',
}))
AutosizeInputContainer.displayName = 'AutosizeInputContainer'

export const defaultStartEnhancer = () => <IconFont type='filter' size={12} kind='gray' />
export const defaultLabelRemoveIcon = () => (
    <IconFont
        type='close'
        style={{
            width: '12px',
            height: '12px',
            borderRadius: '50%',
            backgroundColor: ' rgba(2,16,43,0.20)',
            color: '#FFF',
            // marginLeft: '6px',
        }}
        size={12}
    />
)
export const defalutPlaceholder = (children: React.ReactNode) => (
    <LabelSmall
        $style={{
            color: 'rgba(2,16,43,0.40)',
            position: 'absolute',
            width: '150px',
            height: '22px',
            top: 0,
            left: '8px',
            lineHeight: '22px',
        }}
    >
        {children}
    </LabelSmall>
)

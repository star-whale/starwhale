import { LabelSmall } from 'baseui/typography'
import IconFont from '../IconFont'
import { themedStyled } from '../theme/styletron'

export const SelectorContainer = themedStyled('div', ({ $isEditing }) => ({
    'display': 'flex',
    'height': '32px',
    'lineHeight': '20px',
    'alignItems': 'center',
    'padding': '4px',
    'borderRadius': '4px',
    '&::-webkit-scrollbar': {
        height: '4px !important',
    },
    'flexGrow': '0',
    '&:hover': {
        borderColor: '#799EE8 !important',
    },

    'overflowX': 'auto',
    'overflowY': 'hidden',
    'borderWidth': '1px',
    'borderColor': $isEditing ? '#799EE8' : '#CFD7E6',
}))

export const SelectorItemContainer = themedStyled('div', () => ({
    display: 'flex',
    gap: '10px',
    height: '32px',
    lineHeight: '20px',
    alignItems: 'center',
    flexGrow: '1',
}))

export const StartEnhancer = themedStyled('div', () => ({
    width: 'auto',
    maxWidth: '34px',
    display: 'grid',
    placeItems: 'center',
    flexShrink: 0,
}))

export const Placeholder = themedStyled('div', () => ({
    'position': 'relative',
    'display': 'flex',
    'width': 0,
    'alignItems': 'center',
    '& > div': {
        width: '150px',
    },
}))

export const SelectItemContainer = themedStyled('div', () => ({
    'position': 'relative',
    'display': 'flex',
    'flexWrap': 'nowrap',
    'gap': '1px',
    'cursor': 'pointer',
    'width': 'auto',
    'height': '22px',
    'lineHeight': '22px',
    '&:hover .label': {
        backgroundColor: '#EDF3FF',
    },
}))

export const LabelContainer = themedStyled('div', () => ({
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

export const defaultStartEnhancer = (shareProps) => <IconFont type='filter' size={12} kind='gray' />
export const defaultLabelRemoveIcon = (shareProps) => (
    <IconFont
        type='close'
        style={{
            width: '12px',
            height: '12px',
            borderRadius: '50%',
            backgroundColor: ' rgba(2,16,43,0.20)',
            color: '#FFF',
            marginLeft: '6px',
        }}
        size={12}
    />
)
export const defalutPlaceholder = (children) => (
    <LabelSmall $style={{ color: 'rgba(2,16,43,0.40)', position: 'absolute' }}>{children}</LabelSmall>
)

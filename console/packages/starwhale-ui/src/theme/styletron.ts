import { createThemedStyled, createThemedWithStyle, createThemedUseStyletron } from 'baseui'
import { SWThemeT } from './index'

export const themedStyled = createThemedStyled<SWThemeT>()
export const themedWithStyle = createThemedWithStyle<SWThemeT>()
export const themedUseStyletron = createThemedUseStyletron<SWThemeT>()

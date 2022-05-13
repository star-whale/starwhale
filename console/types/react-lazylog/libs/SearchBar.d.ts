/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable react/static-property-placement */
/* eslint-disable react/prefer-stateless-function */
/* eslint-disable @typescript-eslint/naming-convention */
/* eslint-disable import/no-extraneous-dependencies */
import { Component, ReactNode, CSSProperties } from 'react'

export interface SearchBarProps {
    onSearch?: (keyword: string) => void
    onClearSearch?: () => void
    onFilterLinesWithMatches?: (isFiltered: boolean) => void
    resultsCount?: number
    filterActive?: boolean
    disabled?: boolean
}

export default class SearchBar extends Component<SearchBarProps> {
    static defaultProps: Partial<SearchBarProps>
}

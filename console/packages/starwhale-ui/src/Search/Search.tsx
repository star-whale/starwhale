import { ColumnSchemaDesc } from '@starwhale/core/datastore'
import { Select as BaseSelect, SelectProps, SIZE } from 'baseui/select'
import React from 'react'
import { mergeOverrides } from '../utils'
import FilterRenderer from './FilterRenderer'
import SearchComponent from './SearchComponent'

export interface ISearchProps {
    size?: keyof typeof SIZE
    fields: ColumnSchemaDesc[]
}

export default function Search({ ...props }: ISearchProps) {
    return <FilterRenderer {...props} />
    // eslint-disable-next-line  react/jsx-props-no-spreading
    // return <SearchComponent size={size} {...props} overrides={overrides} />
}

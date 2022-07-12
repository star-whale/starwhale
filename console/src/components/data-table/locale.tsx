/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/

export type DataTableLocaleT = {
    emptyState: string
    loadingState: string
    searchAriaLabel: string
    filterAdd: string
    filterExclude: string
    filterExcludeRange: string
    filterExcludeValue: string
    filterApply: string
    filterAppliedTo: string
    optionsLabel: string
    optionsSearch: string
    optionsEmpty: string
    categoricalFilterSelectAll: string
    categoricalFilterSelectClear: string
    categoricalFilterEmpty: string
    categoricalTagSelectOrCreate: string
    datetimeFilterRange: string
    datetimeFilterRangeDatetime: string
    datetimeFilterRangeDate: string
    datetimeFilterRangeTime: string
    datetimeFilterCategorical: string
    datetimeFilterCategoricalWeekday: string
    datetimeFilterCategoricalMonth: string
    datetimeFilterCategoricalQuarter: string
    datetimeFilterCategoricalHalf: string
    datetimeFilterCategoricalFirstHalf: string
    datetimeFilterCategoricalSecondHalf: string
    datetimeFilterCategoricalYear: string
    numericalFilterRange: string
    numericalFilterSingleValue: string
    booleanFilterTrue: string
    booleanFilterFalse: string
    booleanColumnTrueShort: string
    booleanColumnFalseShort: string
}

const locale = {
    emptyState: 'No rows match the filter criteria defined. Please remove one or more filters to view more data.',
    loadingState: 'Loading rows.',
    searchAriaLabel: 'Search by text',
    filterAdd: 'Filters',
    filterExclude: 'Exclude',
    filterApply: 'Apply',
    filterExcludeRange: 'Exclude range',
    filterExcludeValue: 'Exclude value',
    filterAppliedTo: 'filter applied to',
    optionsLabel: 'Select column to filter by',
    optionsSearch: 'Search for a column to filter by...',
    optionsEmpty: 'No columns available.',
    categoricalFilterSelectAll: 'Select All',
    categoricalFilterSelectClear: 'Clear',
    categoricalFilterEmpty: 'No categories found',
    categoricalTagSelectOrCreate: 'Select or create a new category',
    datetimeFilterRange: 'Range',
    datetimeFilterRangeDatetime: 'Date, Time',
    datetimeFilterRangeDate: 'Date',
    datetimeFilterRangeTime: 'Time',
    datetimeFilterCategorical: 'Categorical',
    datetimeFilterCategoricalWeekday: 'Weekday',
    datetimeFilterCategoricalMonth: 'Month',
    datetimeFilterCategoricalQuarter: 'Quarter',
    datetimeFilterCategoricalHalf: 'Half',
    datetimeFilterCategoricalFirstHalf: 'H1',
    datetimeFilterCategoricalSecondHalf: 'H2',
    datetimeFilterCategoricalYear: 'Year',
    numericalFilterRange: 'Range',
    numericalFilterSingleValue: 'Single Value',
    booleanFilterTrue: 'true',
    booleanFilterFalse: 'false',
    booleanColumnTrueShort: 'T',
    booleanColumnFalseShort: 'F',
}

export default locale

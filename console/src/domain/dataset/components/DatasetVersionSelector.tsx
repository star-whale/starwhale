import { Select, SelectProps, SIZE } from 'baseui/select'
import _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { useQuery } from 'react-query'
import { listDatasetVersions } from '../services/datasetVersion'

export interface IDatasetVersionSelectorProps {
    projectId: string
    datasetId: string
    value?: string
    onChange?: (newValue: string) => void
    overrides?: SelectProps['overrides']
    disabled?: boolean
    autoSelected?: boolean
}

export default function DatasetVersionSelector({
    projectId,
    datasetId,
    value,
    onChange,
    overrides,
    disabled,
    autoSelected,
}: IDatasetVersionSelectorProps) {
    const [keyword, setKeyword] = useState<string>()
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
    const datasetVersionsInfo = useQuery(
        `listDatasetVersions:${projectId}:${datasetId}:${keyword}`,
        () => listDatasetVersions(projectId, datasetId, { pageNum: 1, pageSize: 100, search: keyword }),
        { enabled: !!datasetId }
    )

    const handleDatasetVersionInputChange = _.debounce((term: string) => {
        if (!term) {
            setOptions([])
            return
        }
        setKeyword(term)
    })

    useEffect(() => {
        if (datasetVersionsInfo.isSuccess) {
            const ops =
                datasetVersionsInfo.data?.list.map((item) => ({
                    id: item.id,
                    label: [item.alias, item.name].join(' : '),
                })) ?? []
            setOptions(ops)
            if (!value && autoSelected) {
                onChange?.(ops[0]?.id)
            }
        } else {
            setOptions([])
        }
    }, [datasetVersionsInfo.data?.list, datasetVersionsInfo.isSuccess, value, onChange])

    return (
        <Select
            size={SIZE.compact}
            disabled={disabled}
            overrides={overrides}
            isLoading={datasetVersionsInfo.isFetching}
            options={options}
            onChange={(params) => {
                if (!params.option) {
                    return
                }
                onChange?.(params.option.id as string)
            }}
            onInputChange={(e) => {
                const target = e.target as HTMLInputElement
                handleDatasetVersionInputChange(target.value)
            }}
            value={
                value
                    ? [
                          {
                              id: value,
                          },
                      ]
                    : []
            }
        />
    )
}

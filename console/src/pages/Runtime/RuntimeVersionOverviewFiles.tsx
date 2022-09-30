import React from 'react'
import Table from '@/components/Table/index'
import useTranslation from '@/hooks/useTranslation'
import { useRuntimeVersionLoading, useRuntimeVersion } from '@/domain/runtime/hooks/useRuntimeVersion'
import { IRuntimeFileSchema } from '@/domain/runtime/schemas/runtime'

export default function RuntimeVersionOverviewFiles() {
    const { runtimeVersion } = useRuntimeVersion()
    const { runtimeVersionLoading } = useRuntimeVersionLoading()

    const [t] = useTranslation()

    return (
        <Table
            isLoading={runtimeVersionLoading}
            columns={[t('File'), t('Size')]}
            data={runtimeVersion?.files?.map((file: IRuntimeFileSchema) => [file?.name, file?.size]) ?? []}
        />
    )
}

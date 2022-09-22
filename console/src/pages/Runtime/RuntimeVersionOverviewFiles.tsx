import React from 'react'
import Table from '@/components/Table/index'
import useTranslation from '@/hooks/useTranslation'
import { useRuntime, useRuntimeLoading } from '@/domain/runtime/hooks/useRuntime'
import { IRuntimeFileSchema } from '@/domain/runtime/schemas/runtime'

export default function RuntimeVersionOverviewFiles() {
    const { runtime } = useRuntime()
    const { runtimeLoading } = useRuntimeLoading()

    const [t] = useTranslation()

    return (
        <Table
            isLoading={runtimeLoading}
            columns={[t('File'), t('Size')]}
            data={runtime?.files?.map((file: IRuntimeFileSchema) => [file?.name, file?.size]) ?? []}
        />
    )
}

import React from 'react'
import Table from '@/components/Table/index'
import useTranslation from '@/hooks/useTranslation'
import { useRuntime, useRuntimeLoading } from '@/domain/runtime/hooks/useRuntime'
import Card from '@/components/Card'
import { IRuntimeFileSchema } from '@/domain/runtime/schemas/runtime'

export default function RuntimeOverview() {
    const { runtime } = useRuntime()
    const { runtimeLoading } = useRuntimeLoading()

    const [t] = useTranslation()

    return (
        <Card
            outTitle={t('Files')}
            style={{
                fontSize: '14px',
                padding: '12px 20px',
                marginBottom: '10px',
            }}
        >
            <Table
                isLoading={runtimeLoading}
                columns={[t('File'), t('Size')]}
                data={runtime?.files?.map((file: IRuntimeFileSchema) => [file?.name, file?.size]) ?? []}
            />
        </Card>
    )
}

import React from 'react'
import CreateOnlineEvalForm from '@model/components/CreateOnlineEvalForm'
import useTranslation from '@/hooks/useTranslation'
import Card from '@/components/Card'

export default function OnlineEval() {
    const [t] = useTranslation()

    return (
        <Card title={t('online eval')}>
            <CreateOnlineEvalForm onSubmit={() => {}} />
        </Card>
    )
}

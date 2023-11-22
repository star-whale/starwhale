import React from 'react'
import { useQueryDatastore } from '@starwhale/core/datastore/hooks/useFetchDatastore'
import { QueryTableRequest } from '@starwhale/core/datastore'
import useFineTuneEvaluation from '@/domain/space/hooks/useFineTuneEvaluation'
import EvaluationOverview from './Evaluation/EvaluationOverview'

function FineTuneEvaluationOverview() {
    const { summaryTableQuery } = useFineTuneEvaluation()
    const info = useQueryDatastore(summaryTableQuery as QueryTableRequest)
    return <EvaluationOverview info={info} />
}

export default FineTuneEvaluationOverview

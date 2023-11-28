import React from 'react'
import EvaluationListCard from './Evaluation/EvaluationListCard'
import { api } from '@/api'
import RouteOverview from './RouteOverview'
import useFineTuneEvaluation from '@/domain/space/hooks/useFineTuneEvaluation'
import EvalJobActionGroup from '@/domain/space/components/EvalJobActionGroup'
import { useCreation } from 'ahooks'
import { GridResizer } from '@starwhale/ui'
import ChatGroup from '@starwhale/ui/Serving/components/ChatGroup'

export default function FineTuneOnlineEvalServings() {
    return <ChatGroup />
}

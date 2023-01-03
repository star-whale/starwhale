import React, { useEffect } from 'react'
import CreateOnlineEvalForm from '@model/components/CreateOnlineEvalForm'
import useTranslation from '@/hooks/useTranslation'
import Card from '@/components/Card'
import { useProject } from '@/domain/project/hooks/useProject'
import { getToken } from '@/api'
import { useParams } from 'react-router'
import { fetchModel } from '@/domain/model/services/model'
import { useQuery } from 'react-query'
import { useFetchModelVersion } from '@/domain/model/hooks/useFetchModelVersion'

// import('http://127.0.0.1:8080/index.js')
// @ts-ignore
import('http://localhost:3000/src/main.ts')

export default function OnlineEval() {
    const { project } = useProject()
    const [t] = useTranslation()
    const [config, setConfig] = React.useState<any>(null)
    const { projectId, modelId, modelVersionId } = useParams<{
        projectId: string
        modelId: string
        modelVersionId: string
    }>()
    const modelInfo = useQuery(`fetchModel:${projectId}:${modelId}`, () => fetchModel(projectId, modelId))
    const modelVersionInfo = useFetchModelVersion(projectId, modelId, modelVersionId)

    useEffect(() => {
        if (modelInfo.isSuccess || modelVersionInfo.isSuccess) {
            const versionName = modelVersionId ? modelVersionInfo?.data?.versionName : modelInfo?.data?.versionName
            const modelName = modelInfo?.data?.name

            fetch(`/api/v1/project/${project?.name}/model/${modelName}/version/${versionName}/file`, {
                headers: {
                    'Authorization': getToken(),
                    'X-SW-DOWNLOAD-OBJECT-PATH': 'src/svc.json',
                    'X-SW-DOWNLOAD-OBJECT-NAME': 'svc.json',
                    'X-SW-DOWNLOAD-OBJECT-HASH': '',
                },
            })
                .then((res) => res.json())
                .then((data) => {
                    setConfig(data)
                    // @ts-ignore
                    window.gradio_config = data
                })
        }
    }, [project?.name, projectId, modelId, modelVersionId, modelInfo.isSuccess, modelVersionInfo.isSuccess])

    console.log(config)

    useEffect(() => {
        window.wait = async () => {
            console.log('wait')
        }
        return () => {
            window.wait = null
        }
    })

    return (
        <>
            <Card title={t('online eval')}>
                <CreateOnlineEvalForm onSubmit={() => {}} />
            </Card>
            {config && (
                <gradio-app>
                    <div id='online-eval' />
                </gradio-app>
            )}
            {/* <iframe src='http://localhost:3000/' title='gradio' height='600px' frameBorder='0' /> */}
        </>
    )
}

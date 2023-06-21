import Card from '@/components/Card'
import React, { useCallback } from 'react'
import Editor from '@monaco-editor/react'
import Button from '@starwhale/ui/Button'
import { executeInTask } from '@job/services/job'

export interface IExecutorFormProps {
    project: string
    job: string
    task: string
}

export default function ExecutorForm({ project, job, task }: IExecutorFormProps) {
    const [cmd, setCmd] = React.useState('')
    const [output, setOutput] = React.useState('')
    const [executing, setExecuting] = React.useState(false)

    const handleSubmit = useCallback(async () => {
        setExecuting(true)
        setOutput('')
        const { stdout, stderr } = await executeInTask(project, job, task, cmd)
        setOutput(`${stdout}\n${stderr}`)
        setExecuting(false)
    }, [cmd, job, project, task])

    return (
        <Card>
            <Editor height='10vh' language='shell' theme='vs-dark' onChange={(content) => setCmd(content ?? '')} />
            <Button onClick={handleSubmit} isLoading={executing}>
                Submit
            </Button>
            <br />
            <Editor
                height='20vh'
                language='shell'
                theme='vs-dark'
                options={{ readOnly: true, minimap: { enabled: false } }}
                value={output}
            />
        </Card>
    )
}

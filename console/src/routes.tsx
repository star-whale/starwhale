import React from 'react'
import { BrowserRouter, Switch, Route, Redirect } from 'react-router-dom'
import Header from '@/components/Header'
import ProjectLayout from '@/pages/Project/ProjectLayout'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import { IThemedStyleProps } from '@/theme'
import { useStyletron } from 'baseui'
import { createUseStyles } from 'react-jss'
import Login from '@/pages/Home/Login'
import ProjectOverview from './pages/Project/Overview'
import ProjectListCard from './pages/Project/ProjectListCard'
import BaseLayout from './pages/BaseLayout'
import ModelLayout from './pages/Model/ModelLayout'
import ModelOverview from './pages/Model/Overview'
import ProjectModels from './pages/Project/Models'
import ProjectDatasets from './pages/Project/Datasets'
import ProjectJobs from './pages/Project/Jobs'
import ModelVersionListCard from './pages/Model/ModelVersionListCard'
import ModelVersionLayout from './pages/Model/ModelVersionLayout'
import DatasetVersionListCard from './pages/Dataset/DatasetVersionListCard'
import DatasetVersionLayout from './pages/Dataset/DatasetVersionLayout'
import DatasetLayout from './pages/Dataset/DatasetLayout'
import DatasetOverview from './pages/Dataset/Overview'
import JobLayout from './pages/Job/JobLayout'
import TaskLayout from './pages/Job/TaskLayout'
import JobOverview from './pages/Job/Overview'
import TaskListCard from './pages/Job/TaskListCard'
import JobForm from './domain/job/components/JobForm'
import JobNewCard from './pages/Project/JobNewCard'
import axios from 'axios'

const useStyles = createUseStyles({
    root: ({ theme }: IThemedStyleProps) => ({
        display: 'flex',
        flexDirection: 'column',
        zIndex: 1,
        minHeight: '100vh',
        position: 'relative',
        minWidth: 'fit-content',
        width: '100%',
        background: 'var(--color-brandRootBackground)',
        color: 'var(--color-contentPrimary)',
        ...Object.entries(theme.colors).reduce((p, [k, v]) => {
            return {
                ...p,
                [`--color-${k}`]: v,
            }
        }, {}),
    }),
})

const Routes = () => {
    const themeType = useCurrentThemeType()
    const [, theme] = useStyletron()
    const styles = useStyles({ theme, themeType })

    return (
        <BrowserRouter>
            <div className={styles.root}>
                <Header />
                <Switch>
                    {/* project */}
                    <Route exact path='/projects/:projectId/:path?'>
                        <ProjectLayout>
                            <Switch>
                                <Route exact path='/projects/:projectId' component={ProjectOverview} />
                                <Route exact path='/projects/:projectId/models' component={ProjectModels} />
                                <Route exact path='/projects/:projectId/datasets' component={ProjectDatasets} />
                                <Route exact path='/projects/:projectId/jobs' component={ProjectJobs} />
                                <Route exact path='/projects/:projectId/new_job' component={JobNewCard} />
                            </Switch>
                        </ProjectLayout>
                    </Route>
                    {/* job & task */}
                    <Route exact path='/projects/:projectId/jobs/:jobId/tasks'>
                        <TaskLayout>
                            <Switch>
                                <Route exact path='/projects/:projectId/jobs/:jobId/tasks' component={TaskListCard} />
                            </Switch>
                        </TaskLayout>
                    </Route>
                    <Route exact path='/projects/:projectId/jobs/:jobId/:path?/:path?'>
                        <JobLayout>
                            <Switch>
                                <Route exact path='/projects/:projectId/jobs/:jobId' component={JobOverview} />
                            </Switch>
                        </JobLayout>
                    </Route>
                    {/* datasets */}
                    <Route exact path='/projects/:projectId/datasets/:datasetId/versions'>
                        <DatasetVersionLayout>
                            <Switch>
                                <Route
                                    exact
                                    path='/projects/:projectId/datasets/:datasetId/versions'
                                    component={DatasetVersionListCard}
                                />
                            </Switch>
                        </DatasetVersionLayout>
                    </Route>
                    <Route exact path='/projects/:projectId/datasets/:datasetId/:path?/:path?'>
                        <DatasetLayout>
                            <Switch>
                                <Route
                                    exact
                                    path='/projects/:projectId/datasets/:datasetId'
                                    component={DatasetOverview}
                                />
                            </Switch>
                        </DatasetLayout>
                    </Route>
                    {/* model */}
                    <Route exact path='/projects/:projectId/models/:modelId/versions'>
                        <ModelVersionLayout>
                            <Switch>
                                <Route
                                    exact
                                    path='/projects/:projectId/models/:modelId/versions'
                                    component={ModelVersionListCard}
                                />
                            </Switch>
                        </ModelVersionLayout>
                    </Route>
                    <Route exact path='/projects/:projectId/models/:modelId/:path?/:path?'>
                        <ModelLayout>
                            <Switch>
                                <Route exact path='/projects/:projectId/models/:modelId' component={ModelOverview} />
                            </Switch>
                        </ModelLayout>
                    </Route>
                    {/* other */}
                    <Route exact path='/login' component={Login} />
                    <Route>
                        <BaseLayout sidebar={undefined}>
                            <Switch>
                                {/* <Route exact path='/' component={Home} /> */}
                                <Route exact path='/projects' component={ProjectListCard} />
                                <Redirect from='/' to='/projects' />
                            </Switch>
                        </BaseLayout>
                    </Route>
                </Switch>
            </div>
        </BrowserRouter>
    )
}

export default Routes

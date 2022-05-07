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
import TaskLayout from './pages/Job/TaskLayout'
import JobOverview from './pages/Job/JobOverview'
import JobNewCard from './pages/Project/JobNewCard'
import JobResult from './pages/Job/JobResult'
import JobsLayout from './pages/Job/JobsLayout'
import JobGridCard from './pages/Job/JobGridCard'

const useStyles = createUseStyles({
    root: ({ theme }: IThemedStyleProps) => ({
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
                    <Route exact path='/projects/:projectId/jobgrids'>
                        <JobsLayout>
                            <Switch>
                                <Route exact path='/projects/:projectId/jobgrids' component={JobGridCard} />
                            </Switch>
                        </JobsLayout>
                    </Route>
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
                    <Route exact path='/projects/:projectId/jobs/:jobId/:path?'>
                        <TaskLayout>
                            <Switch>
                                <Route exact path='/projects/:projectId/jobs/:jobId/tasks' component={JobOverview} />
                                <Route exact path='/projects/:projectId/jobs/:jobId/results' component={JobResult} />
                            </Switch>
                        </TaskLayout>
                    </Route>
                    {/* <Route exact path='/projects/:projectId/jobs/:jobId'>
                        <JobLayout>
                            <Switch></Switch>
                        </JobLayout>
                    </Route> */}
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

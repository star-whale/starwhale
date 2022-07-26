import AdminLayout from '@/pages/Admin/AdminLayout'
import UserManagement from '@/pages/Admin/UserManagement'
import React from 'react'
import { BrowserRouter, Switch, Route, Redirect } from 'react-router-dom'
import ProjectLayout from '@/pages/Project/ProjectLayout'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'
import { IThemedStyleProps } from '@/theme'
import { useStyletron } from 'baseui'
import { createUseStyles } from 'react-jss'
import Login from '@/pages/Home/Login'
import ProjectListCard from './pages/Project/ProjectListCard'
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
import JobNewCard from './pages/Project/JobNewCard'
import JobsLayout from './pages/Job/JobsLayout'
import JobGridCard from './pages/Job/JobGridCard'
import ApiHeader from './api/ApiHeader'
import Pending from './pages/Home/Pending'
import JobTasks from './pages/Job/JobTasks'
import JobResults from './pages/Job/JobResults'
import JobOverviewLayout from './pages/Job/JobOverviewLayout'
import SettingsOverviewLayout from './pages/Settings/SettingsOverviewLayout'
import SettingAgentListCard from './pages/Settings/SettingAgentListCard'
import RuntimeVersionListCard from './pages/Runtime/RuntimeVersionListCard'
import RuntimeVersionLayout from './pages/Runtime/RuntimeVersionLayout'
import RuntimeLayout from './pages/Runtime/RuntimeLayout'
import RuntimeOverview from './pages/Runtime/Overview'
import ProjectRuntimes from './pages/Project/Runtimes'
import JobDAG from './pages/Job/JobDAG'
import ProjectEvaluations from './pages/Project/Evaluations'
import EvaluationOverviewLayout from './pages/Evaluation/EvaluationOverviewLayout'
import EvaluationResults from './pages/Evaluation/EvaluationResults'

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
                <ApiHeader />
                <Switch>
                    {/* setting */}
                    <Route exact path='/settings/:path?'>
                        <SettingsOverviewLayout>
                            <Switch>
                                <Route exact path='/settings/agents' component={SettingAgentListCard} />
                                <Redirect from='/settings/:path?' to='/settings/agents' />
                            </Switch>
                        </SettingsOverviewLayout>
                    </Route>
                    {/*  */}
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
                                <Route exact path='/projects/:projectId/models' component={ProjectModels} />
                                <Route exact path='/projects/:projectId/datasets' component={ProjectDatasets} />
                                <Route exact path='/projects/:projectId/jobs' component={ProjectJobs} />
                                <Route exact path='/projects/:projectId/evaluations' component={ProjectEvaluations} />
                                <Route exact path='/projects/:projectId/runtimes' component={ProjectRuntimes} />
                                <Route exact path='/projects/:projectId/new_job' component={JobNewCard} />
                                <Redirect from='/projects/:projectId' to='/projects/:projectId/models' />
                            </Switch>
                        </ProjectLayout>
                    </Route>
                    {/* evaluation */}
                    <Route exact path='/projects/:projectId/evaluations/:jobId/:path?'>
                        <EvaluationOverviewLayout>
                            <Switch>
                                <Route
                                    exact
                                    path='/projects/:projectId/evaluations/:jobId/results'
                                    component={EvaluationResults}
                                />
                                <Route
                                    exact
                                    path='/projects/:projectId/evaluations/:jobId/tasks'
                                    component={JobTasks}
                                />
                                <Route
                                    exact
                                    path='/projects/:projectId/evaluations/:jobId/actions'
                                    component={JobDAG}
                                />
                                <Redirect
                                    from='/projects/:projectId/evaluations/:jobId'
                                    to='/projects/:projectId/evaluations/:jobId/results'
                                />
                            </Switch>
                        </EvaluationOverviewLayout>
                    </Route>
                    {/* job & task */}
                    <Route exact path='/projects/:projectId/jobs/:jobId/:path?'>
                        <JobOverviewLayout>
                            <Switch>
                                <Route exact path='/projects/:projectId/jobs/:jobId/tasks' component={JobTasks} />
                                <Route exact path='/projects/:projectId/jobs/:jobId/results' component={JobResults} />
                                <Route exact path='/projects/:projectId/jobs/:jobId/actions' component={JobDAG} />
                                <Redirect
                                    from='/projects/:projectId/jobs/:jobId'
                                    to='/projects/:projectId/jobs/:jobId/actions'
                                />
                            </Switch>
                        </JobOverviewLayout>
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
                    {/* runtime */}
                    <Route exact path='/projects/:projectId/runtimes/:runtimeId/versions'>
                        <RuntimeVersionLayout>
                            <Switch>
                                <Route
                                    exact
                                    path='/projects/:projectId/runtimes/:runtimeId/versions'
                                    component={RuntimeVersionListCard}
                                />
                            </Switch>
                        </RuntimeVersionLayout>
                    </Route>
                    <Route exact path='/projects/:projectId/runtimes/:runtimeId/:path?/:path?'>
                        <RuntimeLayout>
                            <Switch>
                                <Route
                                    exact
                                    path='/projects/:projectId/runtimes/:runtimeId'
                                    component={RuntimeOverview}
                                />
                            </Switch>
                        </RuntimeLayout>
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
                    {/* admin */}
                    <Route exact path='/admin/:path?'>
                        <AdminLayout>
                            <Switch>
                                <Route exact path='/admin/users' component={UserManagement} />
                                <Redirect exact from='/admin' to='/admin/users' />
                            </Switch>
                        </AdminLayout>
                    </Route>
                    {/* other */}
                    <Route exact path='/login' component={Login} />
                    <Route exact path='/logout' component={Pending} />
                    <Route>
                        <ProjectLayout>
                            <Switch>
                                <Route exact path='/projects' component={ProjectListCard} />
                                <Redirect from='/' to='/projects' />
                            </Switch>
                        </ProjectLayout>
                    </Route>
                </Switch>
            </div>
        </BrowserRouter>
    )
}

export default Routes

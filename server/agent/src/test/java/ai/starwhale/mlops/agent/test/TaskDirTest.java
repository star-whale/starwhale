package ai.starwhale.mlops.agent.test;

import ai.starwhale.mlops.agent.task.persistence.FileSystemPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static ai.starwhale.mlops.agent.task.persistence.FileSystemPath.*;

public class TaskDirTest {
    @Test
    public void main() {
        FileSystemPath.PathNode baseDir = new FileSystemPath.PathNode("%s", FileSystemPath.PathNode.Type.variable, (FileSystemPath.PathNode) null);


        FileSystemPath.PathNode tasksDir = new FileSystemPath.PathNode("tasks", FileSystemPath.PathNode.Type.value, baseDir);

        FileSystemPath.PathNode activeTaskDir = new FileSystemPath.PathNode("active", FileSystemPath.PathNode.Type.value, tasksDir);

        FileSystemPath.PathNode activeEvaluationTaskDir = new FileSystemPath.PathNode("evaluation", FileSystemPath.PathNode.Type.value, activeTaskDir);
        FileSystemPath.PathNode oneActiveEvaluationTaskDir = new FileSystemPath.PathNode("%s", FileSystemPath.PathNode.Type.variable, activeEvaluationTaskDir);
        FileSystemPath.PathNode taskInfoFile = new FileSystemPath.PathNode("taskInfo.json", FileSystemPath.PathNode.Type.value, oneActiveEvaluationTaskDir);
        FileSystemPath.PathNode taskStatusDir = new FileSystemPath.PathNode("status", FileSystemPath.PathNode.Type.value, oneActiveEvaluationTaskDir);
        FileSystemPath.PathNode taskStatusFile = new FileSystemPath.PathNode("current", FileSystemPath.PathNode.Type.value, taskStatusDir);
        FileSystemPath.PathNode swdsConfigDir = new FileSystemPath.PathNode("config", FileSystemPath.PathNode.Type.value, oneActiveEvaluationTaskDir);
        FileSystemPath.PathNode swdsConfigFile = new FileSystemPath.PathNode("swds.json", FileSystemPath.PathNode.Type.value, swdsConfigDir);
        FileSystemPath.PathNode taskResultDir = new FileSystemPath.PathNode("result", FileSystemPath.PathNode.Type.value, oneActiveEvaluationTaskDir);


        FileSystemPath.PathNode activeCompareTaskDir = new FileSystemPath.PathNode("compare", FileSystemPath.PathNode.Type.value, activeTaskDir);
        FileSystemPath.PathNode oneActiveCompareTaskDir = new FileSystemPath.PathNode("%s", FileSystemPath.PathNode.Type.variable, activeCompareTaskDir);


        FileSystemPath.PathNode archivedTaskDir = new FileSystemPath.PathNode("archived", FileSystemPath.PathNode.Type.value, tasksDir);
        FileSystemPath.PathNode archivedEvaluationTaskDir = new FileSystemPath.PathNode("evaluation", FileSystemPath.PathNode.Type.value, archivedTaskDir);
        FileSystemPath.PathNode archivedCompareTaskDir = new FileSystemPath.PathNode("compare", FileSystemPath.PathNode.Type.value, archivedTaskDir);


        FileSystemPath.PathNode swmpBaseDir = new FileSystemPath.PathNode("swmp", FileSystemPath.PathNode.Type.value, baseDir);
        FileSystemPath.PathNode swmpDir = new FileSystemPath.PathNode("%s", FileSystemPath.PathNode.Type.variable, swmpBaseDir);
        FileSystemPath.PathNode swmpVersionDir = new FileSystemPath.PathNode("%s", FileSystemPath.PathNode.Type.variable, swmpDir);

        // base active task dir
        Assertions.assertEquals("var/sw/tasks/active", activeTaskDir.path("var/sw"));
        // evaluation task test
        Assertions.assertEquals("var/sw/tasks/active/evaluation", activeEvaluationTaskDir.path("var/sw"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123", oneActiveEvaluationTaskDir.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/taskInfo.json", taskInfoFile.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/config", swdsConfigDir.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/config/swds.json", swdsConfigFile.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/status", taskStatusDir.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/status/current", taskStatusFile.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/result", taskResultDir.path("var/sw", "123"));

        // compare task test
        Assertions.assertEquals("var/sw/tasks/active/compare", activeCompareTaskDir.path("var/sw"));
        Assertions.assertEquals("var/sw/tasks/active/compare/123", oneActiveCompareTaskDir.path("var/sw", "123"));

        // archived task test
        Assertions.assertEquals("var/sw/tasks/archived", archivedTaskDir.path("var/sw"));
        Assertions.assertEquals("var/sw/tasks/archived/evaluation", archivedEvaluationTaskDir.path("var/sw"));
        Assertions.assertEquals("var/sw/tasks/archived/compare", archivedCompareTaskDir.path("var/sw"));


        // swmp local cache dir test
        Assertions.assertEquals("var/sw/swmp/mnist/v1", swmpVersionDir.path("var/sw", "mnist", "v1"));
    }

    @Test
    public void fs_test() {

        // base active task dir
        Assertions.assertEquals("var/sw/tasks/active", activeTaskDir.path("var/sw"));
        // evaluation task test
        Assertions.assertEquals("var/sw/tasks/active/evaluation", activeEvaluationTaskDir.path("var/sw"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123", oneActiveEvaluationTaskDir.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/taskInfo.json", oneActiveEvaluationTaskInfoFile.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/config", oneActiveEvaluationTaskSwdsConfigDir.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/config/swds.json", oneActiveEvaluationTaskSwdsConfigFile.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/status", oneActiveEvaluationTaskStatusDir.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/status/current", oneActiveEvaluationTaskStatusFile.path("var/sw", "123"));
        Assertions.assertEquals("var/sw/tasks/active/evaluation/123/result", oneActiveEvaluationTaskResultDir.path("var/sw", "123"));

        // compare task test
        Assertions.assertEquals("var/sw/tasks/active/compare", activeCompareTaskDir.path("var/sw"));
        Assertions.assertEquals("var/sw/tasks/active/compare/123", oneActiveCompareTaskDir.path("var/sw", "123"));

        // archived task test
        Assertions.assertEquals("var/sw/tasks/archived", archivedTaskDir.path("var/sw"));
        Assertions.assertEquals("var/sw/tasks/archived/evaluation", archivedEvaluationTaskDir.path("var/sw"));
        Assertions.assertEquals("var/sw/tasks/archived/compare", archivedCompareTaskDir.path("var/sw"));


        // swmp local cache dir test
        Assertions.assertEquals("var/sw/swmp/mnist/v1", swmpVersionDir.path("var/sw", "mnist", "v1"));
    }
}

package ai.starwhale.mlops.domain.project;

import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.user.UserService;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProjectManager {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private UserService userService;

    public ProjectEntity findDefaultProject() {
        Long userId = userService.currentUserDetail().getIdTableKey();
        ProjectEntity defaultProject = projectMapper.findDefaultProject(userId);
        if(defaultProject == null) {
            List<ProjectEntity> entities = projectMapper.listProjectsByOwner(userId);
            if(entities.isEmpty()) {
                log.error("Can not find default project by user, id = {}", userId);
                return null;
            }
            defaultProject = entities.get(0);
        }
        return defaultProject;
    }

    public ProjectEntity findByName(String projectName){
        List<ProjectEntity> projectEntities = projectMapper.listProjects(projectName);
        if(null != projectEntities && !projectEntities.isEmpty()){
            return projectEntities.get(0);
        }
        return findDefaultProject();
    }


}

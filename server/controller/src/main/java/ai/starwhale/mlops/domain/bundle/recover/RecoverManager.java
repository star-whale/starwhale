package ai.starwhale.mlops.domain.bundle.recover;

import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.bundle.BundleURL;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.project.ProjectAccessor;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecoverManager {

    private final ProjectAccessor projectAccessor;
    private final RecoverAccessor recoverAccessor;
    private final IDConvertor idConvertor;

    private RecoverManager(
        ProjectAccessor projectAccessor,
        RecoverAccessor recoverAccessor,
        IDConvertor idConvertor) {
        this.projectAccessor = projectAccessor;
        this.recoverAccessor = recoverAccessor;
        this.idConvertor = idConvertor;
    }

    public static RecoverManager create(ProjectAccessor projectAccessor,
        RecoverAccessor recoverAccessor, IDConvertor idConvertor) {
        return new RecoverManager(projectAccessor, recoverAccessor, idConvertor);
    }

    public Boolean recoverBundle(BundleURL bundleURL) throws RecoverException{
        Long projectId = projectAccessor.getProjectId(bundleURL.getProjectUrl());
        String name = bundleURL.getBundleUrl();
        Long id;
        if(idConvertor.isID(name)) {
            id = idConvertor.revert(name);
            BundleEntity entity = recoverAccessor.findDeletedBundleById(id);
            if(entity == null) {
                throw new RecoverException(String.format("Recover error. Bundle can not be found by id [%s]. ", name));
            }
            name = entity.getName();
        } else {
            // To restore datasets by name, need to check whether there are duplicate names
            List<? extends BundleEntity> list = recoverAccessor.listDeletedBundlesByName(name, projectId);
            if(list.size() > 1) {
                throw new RecoverException(String.format("Recover error. Duplicate names [%s] of deleted bundles. ",  name));
            } else if (list.size() == 0) {
                throw new RecoverException(String.format("Recover error. Can not find deleted bundle [%s].",  name));
            }
            id = list.get(0).getId();
        }

        // Check for duplicate names
        if(recoverAccessor.findByName(name, projectId) != null) {
            throw new RecoverException(String.format("Recover error. Model %s already exists.",  name));
        }

        Boolean res = recoverAccessor.recover(id);
        log.info("Bundle has been recovered. Name={}", name);
        return res;
    }
}

package ai.starwhale.mlops.domain.bundle.revert;

public interface RevertAccessor {

    int revertTo(Long bundleId, Long bundleVersionId);
}

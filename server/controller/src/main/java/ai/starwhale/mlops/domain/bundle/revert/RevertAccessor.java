package ai.starwhale.mlops.domain.bundle.revert;

public interface RevertAccessor {

    Boolean revertTo(Long bundleId, Long bundleVersionId);
}

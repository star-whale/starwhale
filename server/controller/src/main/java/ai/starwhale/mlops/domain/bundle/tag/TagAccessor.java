package ai.starwhale.mlops.domain.bundle.tag;

public interface TagAccessor {

    HasTag findObjectWithTagById(Long id);
    Boolean updateTag(HasTag entity);
}

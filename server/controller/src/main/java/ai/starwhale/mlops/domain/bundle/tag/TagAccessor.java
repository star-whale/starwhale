package ai.starwhale.mlops.domain.bundle.tag;

public interface TagAccessor {

    HasTag findObjectWithTagById(Long id);
    int updateTag(HasTag entity);
}

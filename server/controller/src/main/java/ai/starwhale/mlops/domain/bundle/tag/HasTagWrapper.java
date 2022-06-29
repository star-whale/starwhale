package ai.starwhale.mlops.domain.bundle.tag;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HasTagWrapper implements HasTag {

    private Long id;

    private String name;

    private String tag;
}

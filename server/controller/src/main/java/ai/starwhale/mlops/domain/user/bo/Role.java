package ai.starwhale.mlops.domain.user.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Role implements GrantedAuthority {

    private String roleName;

    private String roleCode;

    @Override
    public String getAuthority() {
        return getRoleCode();
    }

    public static final String NAME_OWNER = "Owner";

    public static final String NAME_MAINTAINER = "Maintainer";

    public static final String NAME_GUEST = "Guest";

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o instanceof Role) {
            Role r = (Role)o;
            return r.getAuthority().equals(((Role) o).getAuthority());
        }
        return false;
    }

}

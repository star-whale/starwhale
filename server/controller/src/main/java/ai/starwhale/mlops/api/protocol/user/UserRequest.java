package ai.starwhale.mlops.api.protocol.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class UserRequest implements Serializable {

    @NotNull
    @JsonProperty("userName")
    private String userName;

    @NotNull
    @JsonProperty("userPwd")
    private String userPwd;
}

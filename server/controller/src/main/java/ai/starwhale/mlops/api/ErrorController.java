package ai.starwhale.mlops.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping(value = { "error"})
    public String getIndex(){
        return "index"; //返回index页面
    }

}

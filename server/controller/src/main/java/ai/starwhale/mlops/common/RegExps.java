package ai.starwhale.mlops.common;

import java.util.regex.Pattern;

public class RegExps {

    public static final String USER_NAME_REGEX = "^[a-zA-Z][a-zA-Z\\d_-]{3,32}$";

    public static final String PROJECT_NAME_REGEX = "^[a-zA-Z][a-zA-Z\\d_-]{3,80}$";

    public static final String BUNDLE_NAME_REGEX = "^[a-zA-Z][a-zA-Z\\d_-]{3,80}$";


    public static void main(String[] args) {
        Pattern pattern = Pattern.compile(USER_NAME_REGEX);
        System.out.println(pattern.matcher("star_whale").matches());
        System.out.println(pattern.matcher("star").matches());
        System.out.println(pattern.matcher("sta").matches());
        System.out.println(pattern.matcher("star123whale").matches());
        System.out.println(pattern.matcher("5starwhale").matches());
        System.out.println(pattern.matcher("_starwhale").matches());
        System.out.println(pattern.matcher("sta%$#rwhale").matches());

        String suffix = ".deleted";
        String name = "ProjectName" + suffix + ".3";
    }
}

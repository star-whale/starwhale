/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.common.util;

import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.util.StringUtils;

public class Md5Util {

    public static String md5(String str, String salt) {
        if(StringUtils.hasText(salt)) {
            str = str + salt;
        }
        return DigestUtil.md5Hex(str);
    }
}

package ai.starwhale.mlops.common.util;

import com.github.pagehelper.Page;
import com.github.pagehelper.Page.Function;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.stream.Collectors;

public class PageUtil {

    public static <T, E> PageInfo<T> toPageInfo(List<E> list, Function<E, T> function) {
        if (list instanceof Page) {
            return ((Page<E>)list).toPageInfo(function);
        } else {
            return PageInfo.of(list.stream().map(function::apply).collect(Collectors.toList()));
        }
    }

}

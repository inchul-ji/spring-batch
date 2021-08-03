package kr.co.starlabs.batch.part3;


import org.springframework.batch.item.ItemProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DuplicateValidationProcessor<T> implements ItemProcessor<T, T> {

    private final Map<String, Object> keyPool = new ConcurrentHashMap<>();
    // <input, output>
    private final Function<T, String> keyExtractor;
    private final boolean allowDuplicate;

    public DuplicateValidationProcessor(Function<T, String> keyExtractor,
                                        boolean allowDuplicate) {
        this.keyExtractor = keyExtractor;
        this.allowDuplicate = allowDuplicate;
    }

    @Override
    public T process(T item) throws Exception {
        if (allowDuplicate) { // 중복체크를 할거냐 말거냐 확인 true: 필터링을 하지말라, false : 필터링을 해라
            return item; // true이면 item을 그냥 넘긴다.
        }

        String key = keyExtractor.apply(item); // key 추출

        if (keyPool.containsKey(key)) {
            return null; // 중복이 발생하면 null
        }

        keyPool.put(key, key);


        return item;
    }


}

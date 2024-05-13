package de.focusshift.zeiterfassung;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class CachedFunction<T, R> implements Function<T, R> {

    private final Map<T, R> cache = new HashMap<>();
    private final Function<T, R> function;

    public CachedFunction(Function<T, R> function) {
        this.function = function;
    }

    @Override
    public R apply(T t) {
        if (cache.containsKey(t)) {
            return cache.get(t);
        }
        return cache.computeIfAbsent(t, function);
    }
}

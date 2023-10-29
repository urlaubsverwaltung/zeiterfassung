package de.focusshift.zeiterfassung;

import org.junit.jupiter.params.provider.Arguments;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

public final class ArgumentsPermutation {

    private ArgumentsPermutation() {
        //
    }

    public static <A, B> Stream<Arguments> of(A[] a, B[] b) {
        return Permutation.of(a, b).map(Arguments::of);
    }

    static class Permutation {

        static <A, B> Stream<Object[]> of(A[] a, B[] b) {
            return of(Arrays.asList(a), Arrays.asList(b));
        }

        static <A, B> Stream<Object[]> of(Collection<A> firstList, Collection<B> secondList) {
            return firstList.stream().flatMap(a -> secondList.stream().map(b -> new Object[]{a, b}));
        }
    }
}

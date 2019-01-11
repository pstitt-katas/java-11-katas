package kata.reactor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class Kata6_Transform {

    @Test
    void separateMethod() {
        Flux<Integer> flux = half(evenSquares(Flux.range(1, 10)));
        Integer[] EXPECTED = {2, 8, 18, 32, 50};

        StepVerifier.create(flux)
                .expectNext(EXPECTED)
                .verifyComplete();
    }

    @Test
    void usingTransform() {
        Flux<Integer> flux = Flux.range(1, 10)
                .transform(this::evenSquares)
                .transform(this::half);
        Integer[] EXPECTED = {2, 8, 18, 32, 50};

        StepVerifier.create(flux)
                .expectNext(EXPECTED)
                .verifyComplete();
    }

    private Flux<Integer> evenSquares(Flux<Integer> source) {
        return source
                .map(i -> i*i)
                .filter(i -> i%2 == 0);
    }

    private Flux<Integer> half(Flux<Integer> source) {
        return source
                .map(i -> i/2);
    }
}

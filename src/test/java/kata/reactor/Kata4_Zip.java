package kata.reactor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;

public class Kata4_Zip {

    @Test
    void zipFlux() {
        Flux<Integer> flux1 = Flux.range(1, 5);
        Flux<Integer> flux2 = Flux.range(0, 5);
        Flux<Tuple2<Integer, Integer>> zipped = flux1.zipWith(flux2);

        StepVerifier.create(zipped)
                .expectNext(
                        Tuples.of(1, 0),
                        Tuples.of(2, 1),
                        Tuples.of(3, 2),
                        Tuples.of(4, 3),
                        Tuples.of(5, 4)
                )
                .verifyComplete();
    }

    @Test
    void zipFlux_mismatched() {
        Flux<Integer> flux1 = Flux.range(1, 5);
        Flux<Integer> flux2 = Flux.range(0, 4); // different number of elements
        Flux<Tuple2<Integer, Integer>> zipped = flux1.zipWith(flux2);

        StepVerifier.create(zipped)
                .expectNext(
                        Tuples.of(1, 0),
                        Tuples.of(2, 1),
                        Tuples.of(3, 2),
                        Tuples.of(4, 3)
                )
                .verifyComplete();
    }

    @Test
    void zipMono() {
        Integer INPUT1 = 1;
        Integer INPUT2 = 2;
        Mono<Integer> mono1 = Mono.just(INPUT1);
        Mono<Integer> mono2 = Mono.just(INPUT2);
        Mono<Tuple2<Integer, Integer>> zippedMono = mono1.zipWith(mono2);

        StepVerifier.create(zippedMono)
                .expectNext(Tuples.of(INPUT1, INPUT2))
                .verifyComplete();
    }

    @Test
    void zipMonoWithDelay() {
        Integer INPUT1 = 1;
        Integer INPUT2 = 2;
        Mono<Integer> mono1 = Mono.just(INPUT1).delayElement(Duration.ofSeconds(1));
        Mono<Integer> mono2 = Mono.just(INPUT2);
        Mono<Tuple2<Integer, Integer>> zippedMono = mono1.zipWith(mono2);

        StepVerifier.create(zippedMono)
                .expectNext(Tuples.of(INPUT1, INPUT2))
                .verifyComplete();
    }
}

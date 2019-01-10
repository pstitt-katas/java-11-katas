package kata.reactor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.time.Duration;

public class Kata4_Zip {

    @Test
    void zipFlux() {
        Flux<Integer> flux1 = Flux.range(1, 5);
        Flux<Integer> flux2 = Flux.range(0, 5);
        Flux<Tuple2<Integer, Integer>> zipped = flux1.zipWith(flux2);

        StepVerifier.create(zipped)
                .expectNextMatches(t -> t.getT1() == 1 && t.getT2() == 0)
                .expectNextMatches(t -> t.getT1() == 2 && t.getT2() == 1)
                .expectNextMatches(t -> t.getT1() == 3 && t.getT2() == 2)
                .expectNextMatches(t -> t.getT1() == 4 && t.getT2() == 3)
                .expectNextMatches(t -> t.getT1() == 5 && t.getT2() == 4)
                .expectComplete()
                .verify();
    }

    @Test
    void zipFlux_mismatched() {
        Flux<Integer> flux1 = Flux.range(1, 5);
        Flux<Integer> flux2 = Flux.range(0, 4); // different number of elements
        Flux<Tuple2<Integer, Integer>> zipped = flux1.zipWith(flux2);

        StepVerifier.create(zipped)
                .expectNextMatches(t -> t.getT1() == 1 && t.getT2() == 0)
                .expectNextMatches(t -> t.getT1() == 2 && t.getT2() == 1)
                .expectNextMatches(t -> t.getT1() == 3 && t.getT2() == 2)
                .expectNextMatches(t -> t.getT1() == 4 && t.getT2() == 3)
                .expectComplete()
                .verify();
    }

    @Test
    void zipMono() {
        Integer INPUT1 = 1;
        Integer INPUT2 = 2;
        Mono<Integer> mono1 = Mono.just(INPUT1);
        Mono<Integer> mono2 = Mono.just(INPUT2);
        Mono<Tuple2<Integer, Integer>> zippedMono = mono1.zipWith(mono2);

        StepVerifier.create(zippedMono)
                .expectNextMatches(t -> t.getT1() == INPUT1 && t.getT2() == INPUT2)
                .expectComplete()
                .verify();
    }

    @Test
    void zipMonoWithDelay() {
        Integer INPUT1 = 1;
        Integer INPUT2 = 2;
        Mono<Integer> mono1 = Mono.just(INPUT1).delayElement(Duration.ofSeconds(1));
        Mono<Integer> mono2 = Mono.just(INPUT2);
        Mono<Tuple2<Integer, Integer>> zippedMono = mono1.zipWith(mono2);

        StepVerifier.create(zippedMono)
                .expectNextMatches(t -> t.getT1() == INPUT1 && t.getT2() == INPUT2)
                .expectComplete()
                .verify();
    }
}

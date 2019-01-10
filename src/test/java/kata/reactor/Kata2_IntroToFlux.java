package kata.reactor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Kata2_IntroToFlux {
    @Nested
    class NoOpTests {
        @Test
        void noOp_using_block() {
            Integer INPUT = 1;
            Integer expected = INPUT;

            Integer output =
                    Flux.just(INPUT)
                            .blockFirst();

            assertEquals(expected, output);
        }

        @Test
        void noOp_using_StepVerifier() {
            Integer INPUT = 1;
            Integer expected = INPUT;
            Flux flux = Flux.just(INPUT);

            StepVerifier.create(flux)
                    .expectNext(expected)
                    .expectComplete()
                    .verify();
        }
    }

    @Nested
    class MappingTests {
        @Test
        void mapIntegerToString() {
            Integer INPUT = 1;
            String expected = Integer.toString(INPUT);
            Flux flux = Flux.just(INPUT)
                    .map(i -> Integer.toString(i));

            StepVerifier.create(flux)
                    .expectNext(expected)
                    .expectComplete()
                    .verify();
        }

        @Test
        void flatMapIntegerFluxToString() {
            Integer INPUT = 1;
            String expected = Integer.toString(INPUT);
            Flux flux = Flux.just(INPUT)
                    .flatMap(i -> Flux.just(Integer.toString(i)));

            StepVerifier.create(flux)
                    .expectNext(expected)
                    .expectComplete()
                    .verify();
        }
    }

    @Nested
    class MultivaluedTests {
        @Test
        void fluxFromRange() {
            Flux<Integer> flux = Flux.range(10, 5);
            Integer[] EXPECTED = {10, 11, 12, 13, 14};

            StepVerifier.create(flux)
                    .expectNext(EXPECTED)
                    .expectComplete()
                    .verify();
        }

        @Test
        void fluxFromCollection() {
            Integer[] EXPECTED = {10, 11, 12, 13, 14};
            Flux<Integer> flux = Flux.fromArray(EXPECTED);

            StepVerifier.create(flux)
                    .expectNext(EXPECTED)
                    .expectComplete()
                    .verify();
        }

        @Test
        void squaredAndEven() {
            Flux<Integer> flux = Flux.range(1, 10)
                    .map(i -> i*i)
                    .filter(i -> i%2 == 0);
            Integer[] EXPECTED = {4, 16, 36, 64, 100};

            StepVerifier.create(flux)
                    .expectNext(EXPECTED)
                    .expectComplete()
                    .verify();
        }
    }
}

package kata.reactor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Kata1_IntroToMono {
    @Nested
    class NoOpTests {
        @Test
        void noOp_using_block() {
            Integer INPUT = 1;
            Integer expected = INPUT;

            Integer output =
                    Mono.just(INPUT)
                            .block();

            assertEquals(expected, output);
        }

        @Test
        void noOp_using_StepVerifier() {
            Integer INPUT = 1;
            Integer expected = INPUT;
            Mono mono = Mono.just(INPUT);

            StepVerifier.create(mono)
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
            Mono mono = Mono.just(INPUT)
                    .map(i -> Integer.toString(i));

            StepVerifier.create(mono)
                    .expectNext(expected)
                    .expectComplete()
                    .verify();
        }

        @Test
        void flatMapIntegerMonoToString() {
            Integer INPUT = 1;
            String expected = Integer.toString(INPUT);
            Mono mono = Mono.just(INPUT)
                    .flatMap(i -> Mono.just(Integer.toString(i)));

            StepVerifier.create(mono)
                    .expectNext(expected)
                    .expectComplete()
                    .verify();
        }
    }
}

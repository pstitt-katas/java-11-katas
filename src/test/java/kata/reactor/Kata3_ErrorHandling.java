package kata.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
public class Kata3_ErrorHandling {

    @Test
    void errorEmittedByMono() {
        String ERROR = "error";
        Mono mono = Mono.error(new RuntimeException(ERROR));

        StepVerifier.create(mono)
                .verifyErrorMatches(x -> x instanceof RuntimeException && x.getMessage().equals(ERROR));
    }

    @Test
    void errorEmittedByMonoAndHandledWithDefaultValue() {
        String ERROR = "error";
        Integer DEFAULT_VALUE = 99;
        Mono mono = Mono.error(new RuntimeException(ERROR))
                .onErrorReturn(DEFAULT_VALUE);

        StepVerifier.create(mono)
                .expectNext(DEFAULT_VALUE)
                .verifyComplete();
    }

    @Test
    void errorEmittedByNestedMono() {
        Integer INPUT = 1;
        String ERROR = "error";
        Mono mono = Mono.just(INPUT)
                .flatMap(i -> Mono.error(new RuntimeException(ERROR)));

        StepVerifier.create(mono)
                .verifyErrorMatches(x -> x instanceof RuntimeException && x.getMessage().equals(ERROR));
    }

    @Test
    void errorEmittedAndHandledByNestedMono() {
        Integer INPUT = 1;
        String DEFAULT_VALUE = "99";
        String ERROR = "error";
        Mono mono = Mono.just(INPUT)
                .flatMap(i ->
                        Mono.error(new RuntimeException(ERROR))
                                .onErrorReturn(DEFAULT_VALUE));

        StepVerifier.create(mono)
                .expectNext(DEFAULT_VALUE)
                .verifyComplete();
    }

    @Test
    void errorEmittedByNestedMonoAndHandledAtOuterMono() {
        Integer INPUT = 1;
        String DEFAULT_VALUE = "99";
        String ERROR = "error";
        Mono mono = Mono.just(INPUT)
                .flatMap(i -> Mono.error(new RuntimeException(ERROR)))
                .onErrorReturn(DEFAULT_VALUE);

        StepVerifier.create(mono)
                .expectNext(DEFAULT_VALUE)
                .verifyComplete();
    }

    @Test
    void divideByZero_then_abort() {
        Flux<Integer> flux = Flux.range(-2, 5)
                .map(i -> (10/i));

        StepVerifier.create(flux)
                .expectNext(-5, -10)
                .verifyError(ArithmeticException.class);
    }

    @Test
    void divideByZero_then_continue() {
        Flux<Integer> flux = Flux.range(-2, 5)
                .map(i -> (10/i))
                .onErrorContinue((error, i) -> log.error("got divide by zero error on element {}", i));

        StepVerifier.create(flux)
                .expectNext(-5, -10, 10, 5)
                .verifyComplete();
    }
}

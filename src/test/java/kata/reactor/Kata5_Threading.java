package kata.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
public class Kata5_Threading {

    @Test
    void defaultThreads() {
        Mono<Collection<String>> threadsMono = Flux.range(1, 5)
                .map(i -> Thread.currentThread().getName())
                .doOnNext(t -> log.info("default thread: {}", t))
                .collectSortedList(String::compareTo)
                .map(this::toSet);

        StepVerifier.create(threadsMono)
                .expectNextMatches(threads -> threads.size() == 1)
                .verifyComplete();
    }

    @Test
    void subscribeOnParallel() {
        Mono<Set<String>> threadsMono = Flux.range(1, 5)
                .map(i -> Thread.currentThread().getName())
                .collectSortedList(String::compareTo)
                .map(this::toSet)
                .subscribeOn(Schedulers.parallel());

        StepVerifier.create(threadsMono)
                .expectNextMatches(threads -> threads.size() == 1 && threadsEachMatch(threads, "parallel-[0-9]+"))
                .verifyComplete();
    }

    @Test
    void publishOnParallel() {
        Mono<Collection<String>> threadsMono = Flux.range(1, 5)
                .publishOn(Schedulers.parallel())
                .map(i -> Thread.currentThread().getName())
                .collectSortedList(String::compareTo)
                .map(this::toSet);

        StepVerifier.create(threadsMono)
                .expectNextMatches(threads -> threads.size() == 1 && threadsEachMatch(threads, "parallel-[0-9]+"))
                .verifyComplete();
    }

    @Test
    void parallelFlux() {
        Mono<Collection<String>> mono = Flux.range(1, 10)
                .parallel()
                .runOn(Schedulers.parallel())
                .map(i -> Thread.currentThread().getName())
                .collectSortedList(String::compareTo)
                .map(this::toSet);
        String EXPECTED_THREAD_REGEX = "parallel-[0-9]+";

        StepVerifier.create(mono)
                .expectNextMatches(threads -> threads.size() > 1 && threadsEachMatch(threads, EXPECTED_THREAD_REGEX))
                .verifyComplete();
    }

    private Set<String> toSet(Collection<String> coll) {
        return coll.stream().collect(Collectors.toSet());
    }

    private boolean threadsEachMatch(Collection<String> threads, String regex) {
        try {
            threads.stream().forEach(thread -> {
                log.info("Thread: {}", thread);
                if (!thread.matches(regex)) {
                    String error = format("Found thread %s, but expected it to contain %s", thread, regex);
                    throw new RuntimeException(error);
                }
            });
        } catch (RuntimeException x) {
            log.error(x.getMessage());
            return false;
        }
        return true;
    }
}

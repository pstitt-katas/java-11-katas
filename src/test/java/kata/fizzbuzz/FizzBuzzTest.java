package kata.fizzbuzz;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FizzBuzzTest {

    @ParameterizedTest
    @MethodSource("multiplesOf3AndNot5")
    void fizz(int number) {
        assertEquals("fizz", say(number));
    }

    @ParameterizedTest
    @MethodSource("multiplesOf5AndNot3")
    void buzz(int number) {
        assertEquals("buzz", say(number));
    }

    @ParameterizedTest
    @MethodSource("notMultiplesOf3Or5")
    void number(int number) {
        assertEquals(Integer.toString(number), say(number));
    }

    @ParameterizedTest
    @MethodSource("multiplesOf3And5")
    void fizzbuzz(int number) {
        assertEquals("fizzbuzz", say(number));
    }

    private String say(int number) {
        boolean multipleOf3 = (number%3 == 0);
        boolean multipleOf5 = (number%5 == 0);
        StringBuilder result = new StringBuilder();

        if (multipleOf3) {
            result.append("fizz");
        }

        if (multipleOf5) {
            result.append("buzz");
        }

        if (result.length() == 0) {
            result.append(Integer.toString(number));
        }

        return result.toString();
    }

    private static Stream<Integer> multiplesOf3AndNot5() {
        return Stream.of(3, 6, 9, 27, 303);
    }

    private static Stream<Integer> multiplesOf5AndNot3() {
        return Stream.of(5, 10, 25, 100, 2000);
    }

    private static Stream<Integer> multiplesOf3And5() {
        return Stream.of(15, 30, 60, 120, 1500);
    }

    private static Stream<Integer> notMultiplesOf3Or5() {
        return Stream.of(1, 7, 8, 26, 304);
    }
}

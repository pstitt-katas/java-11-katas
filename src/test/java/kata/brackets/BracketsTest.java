package kata.brackets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class BracketsTest {

    private static final int NUMBER_OF_GENERATED_INPUTS = 100;
//    private static final int MAX_DEPTH_OF_GENERATED_INPUTS = 10000; // Causes stack overflow in some implementations
    private static final int MAX_DEPTH_OF_GENERATED_INPUTS = 1000;

    private final GrammarChecker checker;

    protected BracketsTest(GrammarChecker checker) {
        this.checker = checker;
    }

    @ParameterizedTest
    @MethodSource({"examplesThatAreBalanced", "generateBalanced"})
    void balanced(String input) {
        assertTrue(isBalanced(input));
    }

    @ParameterizedTest
    @MethodSource({"examplesThatHaveUnbalancedOpenBracket"})
    void unbalancedOpenBracket(String input) {
        assertFalse(isBalanced(input));
    }

    @ParameterizedTest
    @MethodSource({"examplesThatHaveUnbalancedCloseBracket"})
    void unbalancedCloseBracket(String input) {
        assertFalse(isBalanced(input));
    }

    @ParameterizedTest
    @MethodSource({"examplesThatHaveInvalidCharacters"})
    void invalidCharacters(String input) {
        assertFalse(isBalanced(input));
    }

    private boolean isBalanced(String input) {
        try {
            return checker.isBalanced(input);
        }
        catch (ParserException x) {
            return false;
        }
    }

    private static Stream<Arguments> examplesThatAreBalanced() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("[]"),
                Arguments.of("[][]"),
                Arguments.of("[[]]"),
                Arguments.of("[[[[]][]][]]")
        );
    }

    private static Stream<Arguments> examplesThatHaveUnbalancedOpenBracket() {
        return Stream.of(
                Arguments.of("["),
                Arguments.of("[]["),
                Arguments.of("[[[]]"),
                Arguments.of("[[[][]]")
        );
    }

    private static Stream<Arguments> examplesThatHaveUnbalancedCloseBracket() {
        return Stream.of(
                Arguments.of("]"),
                Arguments.of("]["),
                Arguments.of("[[[]][]]]")
        );
    }

    private static Stream<Arguments> examplesThatHaveInvalidCharacters() {
        return Stream.of(
                Arguments.of("[a]"),
                Arguments.of("[1]"),
                Arguments.of("[ ]"),
                Arguments.of("[\t]"),
                Arguments.of("[\n]"),
                Arguments.of("[]\r]"),
                Arguments.of("$")
        );
    }

    private static Stream<Arguments> generateBalanced() {
        Stream.Builder<Arguments> builder = Stream.builder();

        IntStream.range(1, NUMBER_OF_GENERATED_INPUTS).forEach(x -> builder.accept(generateSingleBalanced(MAX_DEPTH_OF_GENERATED_INPUTS)));

        return builder.build();
    }

    private static Arguments generateSingleBalanced(int maxDepth) {
        int size = new Random().nextInt(maxDepth);
        StringBuilder builder = new StringBuilder(size);
        IntStream.range(1, size).forEach(x -> builder.append('['));
        IntStream.range(1, size).forEach(x -> builder.append(']'));
        return Arguments.of(builder.toString());
    }
}

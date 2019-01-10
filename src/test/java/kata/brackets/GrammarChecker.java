package kata.brackets;

import java.util.stream.Stream;

public interface GrammarChecker {

    default boolean isBalanced(String input) {
        Tokenizer tokenizer = new Tokenizer();
        Stream<Token> tokenStream = tokenizer.getTokens(input);
        return isBalanced(tokenStream);
    }

    boolean isBalanced(Stream<Token> tokenStream);
}

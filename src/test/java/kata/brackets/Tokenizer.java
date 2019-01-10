package kata.brackets;

import java.util.stream.Stream;

public class Tokenizer {
    public Stream<Token> getTokens(String input) {
        Stream.Builder<Token> stream = Stream.builder();
        for (char ch : input.toCharArray()) {
            stream.accept(Token.of(ch));
        }
        return stream.build();
    }
}

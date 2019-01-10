package kata.brackets.oo;

import kata.brackets.GrammarChecker;
import kata.brackets.ParserException;
import kata.brackets.Token;
//import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

//@Slf4j
public class Parser implements GrammarChecker {
    private ParseContext context = new ParseContext();

    @Override
    public boolean isBalanced(Stream<Token> tokenStream) {
        try {
            tokenStream.forEach(this::processToken);
        }
        catch (ParserException x) {
            return false;
        }

        return context.getDepth() == 0;
    }

    private void processToken(Token token) {
        context = handlerRegistry.getHandler(token).apply(context);
    }

    private static final TokenHandlerRegistry handlerRegistry = new TokenHandlerRegistry();
}

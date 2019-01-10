package kata.brackets.functional;

import kata.brackets.GrammarChecker;
import kata.brackets.Token;
import kata.brackets.oo.ParseContext;

import java.util.stream.Stream;

public class ReducingChecker implements GrammarChecker {

    @Override
    public boolean isBalanced(Stream<Token> tokenStream) {
        // A bit of a bastardisation of the 3 argument form of reduce, but it works. TODO Find a more elegant way.
        return tokenStream.reduce(
                new ParseContext(),
                (ctx, token) -> updateContext(token, ctx),
                (t1,t2) -> t2
        )
        .getDepth() == 0;
    }

    private ParseContext updateContext(Token token, ParseContext context) {
        if (token == Token.OPEN_BRACKET) {
            context = context.startSubcontext();
        }
        else if (token == Token.CLOSE_BRACKET) {
            context = context.endSubcontext();
        }
        return context;
    }
}

package kata.brackets.functional;

import kata.brackets.GrammarChecker;
import kata.brackets.Token;
import kata.brackets.oo.ParseContext;

import java.util.Iterator;
import java.util.stream.Stream;

public class RecursiveCheckerWithObjects implements GrammarChecker {

    @Override
    public boolean isBalanced(Stream<Token> tokenStream) {
        return isBalanced(tokenStream.iterator(), new ParseContext());
    }

    private boolean isBalanced(Iterator<Token> tokenIterator, ParseContext context) {
        int depth = context.getDepth();
        if (reachedEndOfStream(tokenIterator) || isDepthInvalid(depth)) {
            return depth == 0;
        }

        context = updateContext(tokenIterator.next(), context);

        return isBalanced(tokenIterator, context);
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

    private boolean isDepthInvalid(int depth) {
        return depth < 0;
    }

    private static boolean reachedEndOfStream(Iterator i) {
        return !i.hasNext();
    }
}

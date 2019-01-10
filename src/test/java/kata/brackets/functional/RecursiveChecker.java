package kata.brackets.functional;


import kata.brackets.GrammarChecker;
import kata.brackets.Token;

import java.util.stream.Stream;

public class RecursiveChecker implements GrammarChecker {

    @Override
    public boolean isBalanced(String input) {
        return isBalanced(0, input);
    }

    private boolean isBalanced(int nestingDepth, String remainingInput) {
        if (remainingInput.length() == 0 || nestingDepth < 0) {
            return nestingDepth == 0;
        }

        char next = remainingInput.charAt(0);

        if (next == '[') {
            nestingDepth++;
        } else if (next == ']') {
            nestingDepth--;
            if (nestingDepth < 0) {
                return false;
            }
        } else {
            return false;
        }

        return isBalanced(nestingDepth, remainingInput.substring(1));
    }

    @Override
    public boolean isBalanced(Stream<Token> tokenStream) {
        return false;
    }
}

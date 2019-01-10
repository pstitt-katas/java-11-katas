package kata.brackets.oo;

import kata.brackets.ParserException;

public class ParseContext {
    private final ParseContext outerContext;

    public ParseContext() {
        outerContext = null;
    }

    public ParseContext(ParseContext outer) {
        outerContext = outer;
    }

    public ParseContext startSubcontext() {
        return new ParseContext(this);
    }

    public ParseContext endSubcontext() {
        if (outerContext == null) {
            throw new ParserException("Cannot end outer context");
        }
        return outerContext;
    }

    public int getDepth() {
        if (outerContext == null) {
            return 0;
        }

        return outerContext.getDepth() + 1;
    }
}

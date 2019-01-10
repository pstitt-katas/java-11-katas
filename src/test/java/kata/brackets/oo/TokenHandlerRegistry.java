package kata.brackets.oo;

import kata.brackets.Token;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenHandlerRegistry {
    private static final Map<Token, Handler> handlers = registerHandlers();

    public Handler getHandler(Token token) {
        return handlers.get(token);
    }

    private static Map<Token,Handler> registerHandlers() {
        Map<Token, Handler> handlers = new ConcurrentHashMap<>();

        handlers.put(Token.OPEN_BRACKET, ParseContext::startSubcontext);
        handlers.put(Token.CLOSE_BRACKET, ParseContext::endSubcontext);

        return handlers;
    }
}

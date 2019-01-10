package kata.brackets;

//import lombok.extern.slf4j.Slf4j;

//@Slf4j
public enum Token {
    OPEN_BRACKET("["),
    CLOSE_BRACKET("]");

    public String asString() {
        return val;
    }

    public static Token of(char ch) {
        return Token.of(Character.toString(ch));
    }

    public static Token of(String tokenString) {
        for (Token token : Token.values()) {
            if (token.asString().equals(tokenString)) {
                return token;
            }
        }
        throw new InvalidTokenException(tokenString);
    }

    private final String val;

    Token(String val) {
        this.val = val;
    }
}

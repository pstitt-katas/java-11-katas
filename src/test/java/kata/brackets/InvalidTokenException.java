package kata.brackets;

class InvalidTokenException extends ParserException {
    InvalidTokenException(String token) {
        super("Invalid token: '" + token + "'");
    }
}

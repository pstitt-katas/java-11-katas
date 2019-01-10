package kata.brackets.oo;

import java.util.function.UnaryOperator;

@FunctionalInterface
interface Handler extends UnaryOperator<ParseContext> {}

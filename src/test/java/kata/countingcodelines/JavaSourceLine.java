package kata.countingcodelines;

public class JavaSourceLine {
    final String line;

    public JavaSourceLine(String text) {
        this.line = text;
    }

    public boolean isSignificant(JavaContext context) {
        return isSignificant(line, context);
    }

    private static boolean isSignificant(String text, JavaContext context) {
        final String trimmed = text.trim();

        if (context.isInBlockComment()) {
            int endCommentIndex = trimmed.indexOf("*/");
            if (endCommentIndex >= 0) {
                context.endBlockComment();
                String afterComment = trimmed.substring(endCommentIndex+2);
                return isSignificant(afterComment, context);
            }
            else {
                return false;
            }
        }

        if (trimmed.isEmpty()) {
            return false;
        }

        if (trimmed.startsWith("//")) {
            return false;
        }

        if (trimmed.startsWith("/*")) {
            context.startBlockComment();
            String afterStartComment = trimmed.substring(2);
            return isSignificant(afterStartComment, context);
        }

        return true;
    }
}

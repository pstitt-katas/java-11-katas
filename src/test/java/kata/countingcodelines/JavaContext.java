package kata.countingcodelines;

public class JavaContext {
    private boolean inBlockComment;

    public boolean isInBlockComment() {
        return inBlockComment;
    }

    public void startBlockComment() {
        inBlockComment = true;
    }

    public void endBlockComment() {
         inBlockComment = false;
    }
}

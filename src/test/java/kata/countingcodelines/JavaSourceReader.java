package kata.countingcodelines;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.Optional;
import java.util.stream.Stream;

public class JavaSourceReader {
    private final Stream<JavaSourceLine> lines;

    public JavaSourceReader(Reader reader) {
        lines = new BufferedReader(reader).lines().map(line -> new JavaSourceLine(line));
    }

    public int countSignificantLines() {
        final JavaContext context = new JavaContext();

        Optional<Integer> lineCount = lines.map(line -> {
            if (line.isSignificant(context)) {
                return 1;
            }
            else {
                return 0;
            }
        }).reduce((a,b) -> a+b);

        return lineCount.orElse(0);
    }
}

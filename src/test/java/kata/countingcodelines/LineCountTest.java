package kata.countingcodelines;

import jdk.nashorn.api.scripting.URLReader;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineCountTest {
    @Nested
    class NoComments {
        @ParameterizedTest
        @ValueSource(strings = {"class A {", "int i;", "}"})
        void singleLinesWithContent(String lineText) {
            assertEquals(1, countLines(lineText));
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc\ndef\nghi"})
        void multipleLinesWithContent(String lineText) {
            assertEquals(3, countLines(lineText));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t", "  \n\t "})
        void multipleLinesWithWhitespace(String lineText) {
            assertEquals(0, countLines(lineText));
        }
    }

    @Nested
    class SingleLineComments {
        @ParameterizedTest
        @ValueSource(strings = {"//", " //", "\t// blah", "//blah\n//blah\n//"})
        void commentsAtStartOfLine(String lineText) {
            assertEquals(0, countLines(lineText));
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc // blah\ndef // blah", "//blah\nabc\ndef // blah"})
        void commentsAfterContent(String lineText) {
            assertEquals(2, countLines(lineText));
        }
    }

    @Nested
    class MultiLineComments {
        @Nested
        class SingleLine {
            @ParameterizedTest
            @ValueSource(strings = {"/* blah */"})
            void noContent(String lineText) {
                assertEquals(0, countLines(lineText));
            }

            @ParameterizedTest
            @ValueSource(strings = {"abc/* blah */"})
            void afterContent(String lineText) {
                assertEquals(1, countLines(lineText));
            }

            @ParameterizedTest
            @ValueSource(strings = {"/* blah */abc"})
            void beforeContent(String lineText) {
                assertEquals(1, countLines(lineText));
            }

            @ParameterizedTest
            @ValueSource(strings = {"abc /* blah */ def"})
            void betweenContent(String lineText) {
                assertEquals(1, countLines(lineText));
            }
        }

        @Nested
        class MultipleLines {
            @ParameterizedTest
            @ValueSource(strings = {"/* blah\nblah */"})
            void noContent(String lineText) {
                assertEquals(0, countLines(lineText));
            }
        }
    }

    @Test
    void exampleFile() {
        assertEquals(3101, countLinesInFile("/HireCars4USession.java"));
    }

    private int countLinesInFile(String resource) {
        URL url = this.getClass().getResource(resource);
        JavaSourceReader reader = new JavaSourceReader(new URLReader(url));
        return reader.countSignificantLines();
    }

    private int countLines(String text) {
        JavaSourceReader reader = new JavaSourceReader(new StringReader(text));
        return reader.countSignificantLines();
    }
}

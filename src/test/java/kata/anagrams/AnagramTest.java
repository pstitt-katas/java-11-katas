package kata.anagrams;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AnagramTest {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
            // Needed to allow @ParameterizedTest @MethodSource in inner class
    class WordComparisons {
        @ParameterizedTest
        @ValueSource(strings = {"this", "that", "other"})
        void wordsAreAnagramsOfThemselves(String value) {
            Word thisWord = new Word(value);
            assertTrue(thisWord.isAnagramOf(thisWord));
        }

        @ParameterizedTest
        @MethodSource({"simpleAnagrams", "anagramsWithApostrophes", "anagramsWithDifferentCasing"})
        void anagrams(String thisValue, String thatValue) {
            Word thisWord = new Word(thisValue);
            Word thatWord = new Word(thatValue);
            assertTrue(thisWord.isAnagramOf(thatWord));
        }

        @ParameterizedTest
        @MethodSource({"nonAnagrams"})
        void nonAnagrams(String thisValue, String thatValue) {
            Word thisWord = new Word(thisValue);
            Word thatWord = new Word(thatValue);
            assertFalse(thisWord.isAnagramOf(thatWord));
        }

        private Stream simpleAnagrams() {
            return Stream.of(
                    Arguments.of("dad", "add"),
                    Arguments.of("wear", "ware")
            );
        }

        private Stream nonAnagrams() {
            return Stream.of(
                    Arguments.of("add", "subtract")
            );
        }

        private Stream anagramsWithApostrophes() {
            return Stream.of(
                    Arguments.of("a's", "as")
            );
        }

        private Stream anagramsWithDifferentCasing() {
            return Stream.of(
                    Arguments.of("ADD", "dad")
            );
        }
    }

    @Nested
    class AnagramGrouping {
        @Test
        void noWordsProduceNoGroups() {
            assertTrue(group().isEmpty());
        }

        @Test
        void twoAnagramsProduceOneGroupOfTwo() {
            AnagramGroups groups = group("add", "dad");
            assertEquals(1, groups.numberOfGroups());
            Words group = groups.getGroup("add");
            assertNotNull(group);
            assertEquals(2, group.size());
        }

        @Test
        void noAnagramsProduceOneGroupPerWord() {
            AnagramGroups groups = group("add", "subtract");
            assertEquals(2, groups.numberOfGroups());
        }

        AnagramGroups group(String... words) {
            AnagramGroups groups = new AnagramGroups();
            groups.add(new Words(words));
            return groups;
        }
    }

    @Nested
    class FileProcessing {
        private WordFileReader reader = new WordFileReader();

        @Test
        void handleFileNotExisting() {
            assertThrows(IOException.class, () -> reader.loadAnagramGroups(new URL("http://codekata.com/data/bad.url")));
        }

        @Test
        void groupAnagramsFromSampleFile() throws IOException {
            AnagramGroups groups = reader.loadAnagramGroupsFromResource("/wordlist.txt");

            assertEquals(49960, groups.numberOfGroupsWithMoreThanOneWord());

            Words sunderGroup = groups.getGroup("sunder");
            assertNotNull(sunderGroup);
            assertEquals(2, sunderGroup.size());

            Words pasteGroup = groups.getGroup("paste");
            assertNotNull(pasteGroup);
            assertEquals(11, pasteGroup.size());
            pasteGroup.foreach(System.out::println);
        }
    }
}

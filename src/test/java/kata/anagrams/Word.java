package kata.anagrams;

import java.util.Arrays;

public class Word {
    final String value;

    public Word(String value) {
        this.value = value;
    }

    public boolean isAnagramOf(Word thatWord) {
        return getNormalisedValue().equals(thatWord.getNormalisedValue());
    }

    @Override
    public String toString() {
        return value;
    }

    String getNormalisedValue() {
        String filteredValue =
                removeApostrophes(value)
                .toLowerCase();
        return sortCharacters(filteredValue);
    }

    private static String sortCharacters(String filteredValue) {
        char[] chars = filteredValue.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    private static String removeApostrophes(String value) {
        return value.replace("'", "");
    }
}

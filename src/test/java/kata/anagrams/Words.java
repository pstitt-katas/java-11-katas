package kata.anagrams;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class Words {
    private Set<Word> words = new HashSet<>();

    public Words(String... words) {
        for (String word : words) {
            add(word);
        }
    }

    public void add(String word) {
        add(new Word(word));
    }

    public void add(Word word) {
        words.add(word);
    }

    public int size() {
        return words.size();
    }

    public void foreach(Consumer<? super Word> action) {
        words.forEach(action);
    }
}

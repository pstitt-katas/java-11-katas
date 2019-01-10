package kata.anagrams;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AnagramGroups {
    Map<String, Words> groups = new ConcurrentHashMap<>();

    public void add(Word word) {
        String key = word.getNormalisedValue();
        final Words group;
        if (groups.containsKey(key)) {
            group = groups.get(key);
        }
        else {
            group = new Words();
            groups.put(key, group);
        }
        group.add(word);
    }

    public void add(Words words) {
        words.foreach(this::add);
    }

    public int numberOfGroups() {
        return groups.size();
    }

    public long numberOfGroupsWithMoreThanOneWord() {
        return groups.values().stream().filter(words -> words.size() > 1).collect(Collectors.counting());
    }

    public Words getGroup(Word word) {
        return groups.get(word.getNormalisedValue());
    }

    public Words getGroup(String word) {
        return getGroup(new Word(word));
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }
}

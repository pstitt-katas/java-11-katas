package kata.anagrams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class WordFileReader {
    public AnagramGroups loadAnagramGroupsFromResource(String path) throws IOException {
        return loadAnagramGroups(this.getClass().getResource(path));
    }

    public AnagramGroups loadAnagramGroups(URL url) throws IOException {
        AnagramGroups groups = new AnagramGroups();
        URLConnection connection = url.openConnection();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()));

        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            groups.add(new Word(inputLine.trim()));
        }

        in.close();

        return groups;
    }
}

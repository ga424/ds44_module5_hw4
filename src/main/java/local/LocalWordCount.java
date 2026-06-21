package local;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

public class LocalWordCount {
    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path input = projectRoot.resolve("input.txt");
        Path output = projectRoot.resolve("output.txt");

        String text = Files.readString(input);
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = normalized.split("\\s+");

        Map<String, Integer> counts = new TreeMap<>();
        for (String t : tokens) {
            if (t == null || t.isEmpty()) continue;
            counts.put(t, counts.getOrDefault(t, 0) + 1);
        }

        try (BufferedWriter w = Files.newBufferedWriter(output)) {
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                w.write(e.getKey() + "\t" + e.getValue());
                w.newLine();
            }
        }

        System.out.println("Wrote " + counts.size() + " tokens to " + output);
    }
}

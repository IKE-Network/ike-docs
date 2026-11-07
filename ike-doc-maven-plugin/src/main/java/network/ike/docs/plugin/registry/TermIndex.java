package network.ike.docs.plugin.registry;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The term index {@code IKE-INDEX.md} and {@code IKE-TOPIC-REGISTRY.md}
 * describe, {@code term-index.yaml}: the reverse mapping from the index
 * terms a topic marks ({@code indexterm:[primary, secondary]},
 * {@code indexterm2:[term]}, {@code ((term))}, {@code (((primary, secondary)))})
 * to the topics that mark them. The registry's keywords say what a topic
 * is about; its index terms say what it discusses.
 *
 * <p>Terms are collected once, when a topic is parsed, and kept on its
 * registry entry as {@code index-terms}, each level joined with
 * {@code " > "}; the index is then derived from the registry alone, so an
 * incremental registry run never reopens a file to rebuild it
 * (IKE-Network/ike-issues#1341).
 *
 * @since 136
 */
public final class TermIndex {

    /** The key under which a level lists the topics indexed at it. */
    public static final String TOPICS_KEY = "_topics";

    /** The separator between the levels of a term as the registry records it. */
    public static final String LEVEL_SEPARATOR = " > ";

    /** A macro, group 1, or an inline term, groups 2 (the concealing third parenthesis) and 3. */
    private static final Pattern TERM = Pattern.compile(
            "(?<!\\w)indexterm2?:\\[([^\\]]*)]|\\(\\((\\(?)([^()]+?)\\)?\\)\\)");

    private TermIndex() {
    }

    /**
     * The index terms one file marks, in order of appearance, each as its
     * levels joined with {@link #LEVEL_SEPARATOR}; a term marked twice is
     * listed once. A visible term, {@code ((term))}, is one level; a
     * concealed one, {@code (((primary, secondary)))}, splits on commas as
     * {@code indexterm:[]} does. Lines that are comments are passed over.
     *
     * @param content the file content
     * @return the terms, empty when the file marks none
     */
    public static List<String> termsOf(String content) {
        List<String> terms = new ArrayList<>();
        for (String line : content.lines().toList()) {
            if (line.startsWith("//")) {
                continue;
            }
            Matcher term = TERM.matcher(line);
            while (term.find()) {
                if (term.group(1) != null) {
                    add(terms, levels(term.group(1)));
                } else if (term.group(2).isEmpty()) {
                    add(terms, List.of(term.group(3).strip()));
                } else {
                    add(terms, levels(term.group(3)));
                }
            }
        }
        return List.copyOf(terms);
    }

    private static List<String> levels(String list) {
        List<String> levels = new ArrayList<>();
        for (String part : list.split(",")) {
            String level = part.strip();
            if (!level.isEmpty()) {
                levels.add(level);
            }
        }
        return levels;
    }

    private static void add(List<String> terms, List<String> levels) {
        if (levels.isEmpty()) {
            return;
        }
        String term = String.join(LEVEL_SEPARATOR, levels);
        if (!terms.contains(term)) {
            terms.add(term);
        }
    }

    /**
     * The term index as an ordered map, ready for YAML, from a registry
     * model whose topic entries carry {@code index-terms}: {@code generated},
     * {@code term-count}, then {@code terms}, a tree of levels in
     * alphabetical order, each level with its {@code _topics} in id order
     * and its sub-levels after.
     *
     * @param registry the registry model, from {@code TopicRegistry.model}
     *                 or {@code TopicRegistry.load}
     * @param now      the generation instant, recorded as a date
     * @return the term index model
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> model(Map<String, Object> registry, Instant now) {
        TreeMap<String, Object> tree = new TreeMap<>();
        int[] count = {0};
        for (Object d : (List<Object>) registry.getOrDefault("domains", List.of())) {
            Map<String, Object> domain = (Map<String, Object>) d;
            for (Object t : (List<Object>) domain.getOrDefault("topics", List.of())) {
                Map<String, Object> topic = (Map<String, Object>) t;
                String id = String.valueOf(topic.get("id"));
                for (Object term : (List<Object>) topic.getOrDefault("index-terms", List.of())) {
                    TreeMap<String, Object> level = tree;
                    for (String part : String.valueOf(term).split(Pattern.quote(LEVEL_SEPARATOR))) {
                        level = (TreeMap<String, Object>) level.computeIfAbsent(part, k -> {
                            count[0]++;
                            return new TreeMap<>();
                        });
                    }
                    List<String> topics = (List<String>) level.computeIfAbsent(TOPICS_KEY, k -> new ArrayList<>());
                    if (!topics.contains(id)) {
                        topics.add(id);
                    }
                }
            }
        }
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("generated", now.atOffset(ZoneOffset.UTC).toLocalDate().toString());
        model.put("term-count", count[0]);
        model.put("terms", ordered(tree, false));
        return model;
    }

    /** A level with {@code _topics} first (the root has none), then its sub-levels in order, each sorted. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> ordered(TreeMap<String, Object> level, boolean withTopics) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (withTopics) {
            List<String> topics = (List<String>) level.get(TOPICS_KEY);
            List<String> sorted = topics == null ? new ArrayList<>() : new ArrayList<>(topics);
            sorted.sort(null);
            out.put(TOPICS_KEY, sorted);
        }
        for (Map.Entry<String, Object> e : level.entrySet()) {
            if (!e.getKey().equals(TOPICS_KEY)) {
                out.put(e.getKey(), ordered((TreeMap<String, Object>) e.getValue(), true));
            }
        }
        return out;
    }

    /**
     * Render the term index as YAML, in the registry's block style.
     *
     * @param model the model from {@link #model}
     * @return the YAML text
     */
    public static String yaml(Map<String, Object> model) {
        return TopicRegistry.yaml(model, "term-index.yaml, generated by idoc:topic-registry from the index terms. Do not edit.");
    }
}

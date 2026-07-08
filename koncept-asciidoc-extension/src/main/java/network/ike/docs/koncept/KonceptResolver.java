/*
 * Copyright © 2026 Knowledge Graphlet / IKE Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.koncept;

import network.ike.docs.konceptcore.KonceptKind;
import org.asciidoctor.ast.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a koncept reference — a name key ({@code HeartFailure}) or a typed id
 * ({@code sctid=…}, {@code uuid=…}, {@code id=…}, {@code nid=…}) — through {@code koncepts.yml},
 * shared by the inline {@code k:} macro ({@link KonceptInlineMacro}) and the {@code koncept-tree}
 * block ({@link KonceptTreeBlockProcessor}) so both forms render identically
 * ({@code IKE-Network/ike-issues#837}).
 *
 * <p>{@code koncepts.yml} is the resolution authority: a name key resolves by identifier; a typed
 * id by a reverse index over the curated definitions. When a definition is found the concept's
 * <b>PublicId is preferred</b> — explicit UUIDs (in datastore order) over a SNOMED-derived UUID —
 * and the reference gains its glossary {@link Resolved#anchor() anchor} and component kind. A typed
 * id not curated falls back to the PublicId it carries (a {@code uuid}/{@code id} array) or, last,
 * a SNOMED-derived UUID; a {@code nid} is a live-store id with no static form.
 */
final class KonceptResolver {

    private static final Logger LOG = LoggerFactory.getLogger(KonceptResolver.class);

    /** A typed target as the inline macro receives it, e.g. {@code sctid=84114007}. */
    private static final Pattern TYPED_TARGET =
            Pattern.compile("^(sctid|uuid|nid|id)\\s*=\\s*(.+)$");

    private KonceptResolver() {
    }

    /**
     * A resolved reference, ready to render on any backend.
     *
     * @param label    the display label (bracket override &gt; definition label &gt; camelCase/value)
     * @param idString the Tinkar identicon idString, or empty when none resolves
     * @param kind     the component kind
     * @param anchor   the glossary anchor identifier when curated, else {@code null}
     * @param identity the identity text (name · PublicId) for the identicon alt / link title
     */
    record Resolved(String label, Optional<String> idString, KonceptKind kind,
                    String anchor, String identity) {
    }

    /**
     * Whether an inline target is a typed id ({@code kind=value}) rather than a name key.
     *
     * @param target the inline macro target
     * @return {@code true} for a typed id
     */
    static boolean isTyped(String target) {
        return target != null && TYPED_TARGET.matcher(target.strip()).matches();
    }

    /**
     * The bare value of a target: the {@code value} of a typed id, or the whole target for a name
     * key — used as a glossary slug when a reference is uncurated.
     *
     * @param target the inline macro target
     * @return the value part, or the target unchanged
     */
    static String bareValue(String target) {
        Matcher m = TYPED_TARGET.matcher(target == null ? "" : target.strip());
        return m.matches() ? m.group(2).trim() : (target == null ? "" : target);
    }

    /**
     * A safe {@code #koncept-…} anchor slug: keeps word characters, dot and dash and replaces
     * anything else (a comma in a multi-UUID id, a stray bracket) with a dash — a curated
     * identifier is already slug-safe (no-op), but an uncurated typed value could otherwise break
     * the link attribute when it is embedded in a re-parsed macro.
     *
     * @param value the raw anchor value
     * @return a slug safe to embed in an attribute
     */
    static String anchorSlug(String value) {
        return value == null ? "" : value.replaceAll("[^\\w.-]", "-");
    }

    /**
     * Resolves the inline macro's raw target — a {@code kind=value} typed id or a name key.
     *
     * @param doc          the document (for {@code koncepts.yml} resolution)
     * @param target       the inline macro target
     * @param bracketLabel the author's optional bracket-override label
     * @return the resolved reference
     */
    static Resolved resolveTarget(Document doc, String target, String bracketLabel) {
        Matcher m = TYPED_TARGET.matcher(target == null ? "" : target.strip());
        if (m.matches()) {
            return resolve(doc, m.group(1), m.group(2).trim(), bracketLabel);
        }
        return resolve(doc, null, target, bracketLabel);
    }

    /**
     * Resolves an already-split reference: {@code kind} is {@code null} for a name key, else one of
     * {@code sctid}/{@code uuid}/{@code nid}/{@code id}; {@code value} is the identifier or the id.
     *
     * @param doc          the document
     * @param kind         the typed token kind, or {@code null} for a name key
     * @param value        the id value or the name-key identifier
     * @param bracketLabel the author's optional bracket-override label
     * @return the resolved reference
     */
    static Resolved resolve(Document doc, String kind, String value, String bracketLabel) {
        String bracket = (bracketLabel != null && !bracketLabel.isBlank()) ? bracketLabel.strip() : null;
        KonceptDefinitionSource source = KonceptDefinitions.forDocument(doc);
        Optional<KonceptDefinition> def = kind == null
                ? source.lookup(value)
                : reverseIndex(doc, source).find(kind, value).flatMap(source::lookup);

        String label;
        KonceptKind konceptKind;
        String anchor;
        List<UUID> uuids;
        if (def.isPresent()) {
            KonceptDefinition d = def.get();
            anchor = d.identifier();
            konceptKind = Optional.ofNullable(d.kind()).map(KonceptKind::fromString).orElse(KonceptKind.CONCEPT);
            label = bracket != null ? bracket
                    : (d.label() != null && !d.label().isBlank()
                        ? d.label() : KonceptInlineMacro.splitCamelCase(d.identifier()));
            uuids = publicId(d);
        } else {
            anchor = null;                      // not curated → no glossary cross-reference
            konceptKind = KonceptKind.CONCEPT;
            label = bracket != null ? bracket
                    : (kind == null ? KonceptInlineMacro.splitCamelCase(value) : value);
            uuids = uncuratedPublicId(kind, value);
        }

        Optional<String> idString = idsEnabled(doc) && !uuids.isEmpty()
                ? Optional.of(KonceptIdentity.idString(uuids)) : Optional.empty();
        return new Resolved(label, idString, konceptKind, anchor, identity(label, uuids, kind, value));
    }

    /** The concept's PublicId UUIDs (explicit UUIDs preferred over the SNOMED-derived one), or empty. */
    private static List<UUID> publicId(KonceptDefinition def) {
        try {
            return KonceptIdentity.resolveUuids(def);
        } catch (RuntimeException e) {
            LOG.debug("koncept: malformed uuids on {}: {}", def.identifier(), e.toString());
            return List.of();
        }
    }

    /**
     * The PublicId for a typed reference <em>not</em> curated in {@code koncepts.yml}: a
     * {@code uuid}/{@code id} carries its PublicId; an {@code sctid} falls back to a SNOMED-derived
     * UUID; a {@code nid} (and a bare name absent from the source) has no static form.
     */
    private static List<UUID> uncuratedPublicId(String kind, String value) {
        if (kind == null) {
            return List.of();                   // a bare name absent from koncepts.yml
        }
        try {
            return switch (kind) {
                case "uuid", "id" -> parseUuids(value);
                case "sctid" -> List.of(SnomedUuids.fromSnomed(value));
                default -> List.of();           // nid
            };
        } catch (RuntimeException e) {
            LOG.debug("koncept: unresolvable {}={}: {}", kind, value, e.toString());
            return List.of();
        }
    }

    /** Parses a single UUID or a comma-joined UUID array into an ordered list (a multi-id PublicId). */
    private static List<UUID> parseUuids(String value) {
        String[] parts = value.split(",");
        List<UUID> uuids = new ArrayList<>(parts.length);
        for (String part : parts) {
            uuids.add(UUID.fromString(part.trim()));
        }
        return uuids;
    }

    /** Honours the {@code :koncept-identicon:} toggle (default on). */
    private static boolean idsEnabled(Document doc) {
        return !"false".equalsIgnoreCase(String.valueOf(doc.getAttribute("koncept-identicon", "true")));
    }

    /**
     * Builds the identity text carried in the identicon {@code alt} and link {@code title} so a
     * concept survives — as name-plus-identity — a copy into a plain-text context. The identity is
     * the <b>PublicId</b> (the concept's UUIDs, preferred over any SNOMED id); a {@code nid}, which
     * has no static PublicId, carries its native id instead.
     */
    private static String identity(String label, List<UUID> uuids, String kind, String value) {
        if (!uuids.isEmpty()) {
            StringBuilder sb = new StringBuilder(label).append(" · ");
            for (int i = 0; i < uuids.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(uuids.get(i));
            }
            return sb.toString();
        }
        if ("nid".equals(kind)) {
            return label + " · nid " + value;
        }
        return label;
    }

    // ── Reverse index (typed id → curated identifier) ───────────────────

    /** Per-document cache of the reverse index over the curated definitions. */
    private static final Map<Document, ReverseIndex> REVERSE_INDEX = new WeakHashMap<>();

    private static synchronized ReverseIndex reverseIndex(Document doc, KonceptDefinitionSource source) {
        return REVERSE_INDEX.computeIfAbsent(doc, d -> ReverseIndex.build(source));
    }

    /**
     * Maps a typed id (SCTID or UUID) back to its curated koncept identifier, so {@code k:sctid=…}
     * and {@code k:uuid=…} resolve through {@code koncepts.yml} exactly as a name key does. Built by
     * enumerating the source's definitions; a source that cannot enumerate yields an empty index and
     * typed references fall back to the id they carry.
     *
     * @param bySctid SNOMED CT id → koncept identifier
     * @param byUuid  lowercase UUID → koncept identifier
     */
    private record ReverseIndex(Map<String, String> bySctid, Map<String, String> byUuid) {

        static ReverseIndex build(KonceptDefinitionSource source) {
            Map<String, String> bySctid = new HashMap<>();
            Map<String, String> byUuid = new HashMap<>();
            Set<String> conceptSctids = new HashSet<>();
            for (String identifier : source.identifiers()) {
                source.lookup(identifier).ifPresent(def -> {
                    boolean concept = Optional.ofNullable(def.kind()).map(KonceptKind::fromString)
                            .orElse(KonceptKind.CONCEPT) == KonceptKind.CONCEPT;
                    if (def.sctid() != null && !def.sctid().isBlank()) {
                        String sctid = def.sctid().strip();
                        // A bare SCTID means the concept — not a description or stamp that shares it.
                        if (concept) {
                            bySctid.put(sctid, identifier);
                            conceptSctids.add(sctid);
                        } else if (!conceptSctids.contains(sctid)) {
                            bySctid.putIfAbsent(sctid, identifier);
                        }
                    }
                    if (def.uuids() != null) {
                        for (String uuid : def.uuids()) {
                            byUuid.putIfAbsent(uuid.strip().toLowerCase(Locale.ROOT), identifier);
                        }
                    }
                });
            }
            return new ReverseIndex(bySctid, byUuid);
        }

        Optional<String> find(String kind, String value) {
            return switch (kind) {
                case "sctid" -> Optional.ofNullable(bySctid.get(value.strip()));
                case "uuid", "id" -> firstUuidMatch(value);
                default -> Optional.empty();     // nid is not curated by UUID
            };
        }

        private Optional<String> firstUuidMatch(String value) {
            for (String part : value.split(",")) {
                String identifier = byUuid.get(part.strip().toLowerCase(Locale.ROOT));
                if (identifier != null) {
                    return Optional.of(identifier);
                }
            }
            return Optional.empty();
        }
    }
}

package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 从 SKILL.md（或任意 .md）加载 skill 定义：YAML front matter（id, name, description, keywords, handlerId） + 可选正文。
 * 与 Agent Skills 的 SKILL.md 格式对齐，keywords 在 front matter 中为列表或逗号分隔。
 */
public class SkillMarkdownLoader {

    private static final Logger log = LoggerFactory.getLogger(SkillMarkdownLoader.class);
    private static final Pattern FRONT_MATTER_DELIM = Pattern.compile("^---\\s*$", Pattern.MULTILINE);

    /**
     * 从 classpath 下某路径加载所有 .md 文件并解析为 Skill 列表；handler 从 registry 按 handlerId 解析。
     *
     * @param locationPattern 例如 "classpath*:skills/*.md"
     */
    public static List<Skill> loadFromClasspath(String locationPattern, SkillHandlerRegistry handlerRegistry) {
        List<Skill> out = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(locationPattern);
            for (Resource r : resources) {
                if (!r.isReadable()) continue;
                try (InputStream is = r.getInputStream()) {
                    String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Skill skill = parseOne(raw, handlerRegistry);
                    if (skill != null) out.add(skill);
                } catch (Exception e) {
                    log.warn("Failed to load skill from {}", r.getFilename(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve resources for {}", locationPattern, e);
        }
        return out;
    }

    /**
     * 解析单篇 md 内容：取第一个 --- 与第二个 --- 之间为 YAML front matter，解析 id/name/description/keywords/handlerId。
     */
    public static Skill parseOne(String markdown, SkillHandlerRegistry handlerRegistry) {
        if (markdown == null || markdown.isBlank() || handlerRegistry == null) return null;
        String[] parts = FRONT_MATTER_DELIM.split(markdown, 3);
        String yaml = (parts.length >= 2) ? parts[1].trim() : "";
        if (yaml.isBlank()) return null;

        String id = getValue(yaml, "id");
        String name = getValue(yaml, "name");
        String description = getValue(yaml, "description");
        String handlerId = getValue(yaml, "handlerId");
        List<String> keywords = getKeywords(yaml);

        if (id == null || id.isBlank() || handlerId == null || handlerId.isBlank()) {
            log.warn("Skill md missing id or handlerId: id={}, handlerId={}", id, handlerId);
            return null;
        }
        SkillHandler handler = handlerRegistry.get(handlerId);
        if (handler == null) {
            log.warn("No handler registered for handlerId={}, skip skill id={}", handlerId, id);
            return null;
        }
        if (name == null) name = id;
        if (description == null) description = "";
        if (keywords == null) keywords = List.of();

        return new Skill(id, name, description, keywords, handler);
    }

    private static String getValue(String yaml, String key) {
        String prefix = key + ":";
        for (String line : yaml.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                String v = trimmed.substring(prefix.length()).trim();
                if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                if (v.startsWith("'") && v.endsWith("'")) v = v.substring(1, v.length() - 1);
                return v.isBlank() ? null : v;
            }
        }
        return null;
    }

    private static List<String> getKeywords(String yaml) {
        List<String> list = new ArrayList<>();
        boolean inKeywords = false;
        for (String line : yaml.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("keywords:")) {
                inKeywords = true;
                String rest = trimmed.substring("keywords:".length()).trim();
                if (!rest.isBlank()) {
                    if (rest.startsWith("[")) {
                        list.addAll(parseListLine(rest));
                    } else {
                        list.addAll(Arrays.stream(rest.split("[,，]")).map(String::trim).filter(s -> !s.isBlank()).toList());
                    }
                }
                continue;
            }
            if (inKeywords) {
                if (trimmed.startsWith("-")) {
                    list.add(trimmed.substring(1).trim());
                } else if (!trimmed.isBlank() && !trimmed.startsWith("#")) {
                    inKeywords = false;
                }
            }
        }
        return list;
    }

    private static List<String> parseListLine(String rest) {
        rest = rest.replaceAll("^\\[|\\]$", "").trim();
        if (rest.isBlank()) return List.of();
        return Arrays.stream(rest.split("[,，]")).map(s -> s.trim().replaceAll("^[\"']|[\"']$", "")).filter(s -> !s.isBlank()).collect(Collectors.toList());
    }
}

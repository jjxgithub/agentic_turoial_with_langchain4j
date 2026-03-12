package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 从 classpath 加载步骤配置（JSON），供 skill handler 外置步骤列表。
 * 约定路径：{@value #DEFAULT_STEPS_PATH_PREFIX}{skillId}.json，例如 skills/steps/report_query.json。
 */
public class StepDefLoader {

    private static final Logger log = LoggerFactory.getLogger(StepDefLoader.class);

    public static final String DEFAULT_STEPS_PATH_PREFIX = "skills/steps/";

    private final ObjectMapper objectMapper;
    private final String pathPrefix;

    public StepDefLoader(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_STEPS_PATH_PREFIX);
    }

    public StepDefLoader(ObjectMapper objectMapper, String pathPrefix) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.pathPrefix = pathPrefix != null ? pathPrefix : DEFAULT_STEPS_PATH_PREFIX;
    }

    /**
     * 按 skillId 加载步骤列表。文件不存在或解析失败时返回 null，调用方可用内置默认步骤兜底。
     *
     * @param skillId 与文件名对应，如 report_query → skills/steps/report_query.json
     * @return 解析后的 StepDef 列表，或 null
     */
    public List<StepDef> load(String skillId) {
        if (skillId == null || skillId.isBlank()) return null;
        String path = pathPrefix + skillId.trim() + ".json";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                log.debug("No steps config at classpath:{}, skip external steps", path);
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return parse(raw);
            }
        } catch (Exception e) {
            log.warn("Failed to load steps from classpath:{}, use fallback", path, e);
            return null;
        }
    }

    /**
     * 解析 JSON 为 List&lt;StepDef&gt;。期望格式：{ "steps": [ { "id", "name", "agentId", "preProcessorId?", "postProcessorId?", "catchBeforeStepError?", "catchAgentError?", "catchAfterStepError?", "agentRetryCount?", "toolIds"? } ] }
     */
    public List<StepDef> parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode stepsNode = root != null && root.has("steps") ? root.get("steps") : root;
            if (stepsNode == null || !stepsNode.isArray()) return null;
            List<StepDef> out = new ArrayList<>();
            for (JsonNode node : stepsNode) {
                StepDef step = parseOne(node);
                if (step != null) out.add(step);
            }
            return out.isEmpty() ? null : Collections.unmodifiableList(out);
        } catch (Exception e) {
            log.warn("Failed to parse steps JSON", e);
            return null;
        }
    }

    private StepDef parseOne(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        String id = text(node, "id");
        String agentId = text(node, "agentId");
        if (id == null || id.isBlank() || agentId == null || agentId.isBlank()) return null;
        String name = text(node, "name");
        if (name == null) name = id;
        String preProcessorId = text(node, "preProcessorId");
        String postProcessorId = text(node, "postProcessorId");
        if (preProcessorId == null) preProcessorId = "";
        if (postProcessorId == null) postProcessorId = "";
        boolean catchBeforeStepError = node.has("catchBeforeStepError") && node.get("catchBeforeStepError").asBoolean(false);
        boolean catchAgentError = node.has("catchAgentError") && node.get("catchAgentError").asBoolean(false);
        boolean catchAfterStepError = node.has("catchAfterStepError") && node.get("catchAfterStepError").asBoolean(false);
        int agentRetryCount = node.has("agentRetryCount") ? node.get("agentRetryCount").asInt(0) : 0;
        if (agentRetryCount < 0) agentRetryCount = 0;
        List<String> toolIds = null;
        if (node.has("toolIds") && node.get("toolIds").isArray()) {
            List<String> list = new ArrayList<>();
            node.get("toolIds").forEach(t -> list.add(t.asText("")));
            toolIds = list.stream().filter(s -> s != null && !s.isBlank()).toList();
        }
        return new StepDef(id, name, agentId, preProcessorId, postProcessorId, catchBeforeStepError, catchAgentError, catchAfterStepError, agentRetryCount, toolIds != null ? toolIds : List.of());
    }

    private static String text(JsonNode node, String key) {
        if (!node.has(key)) return null;
        JsonNode v = node.get(key);
        return v == null || v.isNull() ? null : v.asText(null);
    }
}

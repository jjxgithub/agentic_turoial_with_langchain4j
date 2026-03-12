package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 通过反射调用带 {@link Tool} 注解的方法，支持单参（String）和多参（由 JSON 入参按参数名/顺序绑定）。
 * 供 {@link GroupTool} 等通用执行器使用。
 */
public final class ToolInvoker {

    private static final Logger log = LoggerFactory.getLogger(ToolInvoker.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private ToolInvoker() {}

    /**
     * 统一入口：根据 tool 的 @Tool 方法签名，用 input 调用并返回 String。
     * <ul>
     *   <li>无参方法：直接调用</li>
     *   <li>单 String 参数：整段 input 作为参数（兼容原行为）</li>
     *   <li>多参数或单非 String 参数：将 input 视为 JSON 对象，按参数名（或 @P 描述）绑定后调用</li>
     * </ul>
     *
     * @return 返回值转成 String；无合适方法或调用异常时返回 null
     */
    public static String invoke(Object tool, String input) {
        if (tool == null) return null;
        Method m = findToolMethod(tool.getClass());
        if (m == null) return null;
        try {
            m.setAccessible(true);
            Object[] args = resolveArgs(m, input);
            if (args == null) return null;
            Object out = m.invoke(tool, args);
            return out == null ? null : out.toString();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Tool invoke failed, tool={}", tool != null ? tool.getClass().getSimpleName() : "null", e);
            }
            return null;
        }
    }

    /**
     * 在 tool 实例上查找「带 @Tool、单 String 参数、返回 String 或可转 String」的方法并调用。
     * 保留用于仅需单参调用的场景；一般请使用 {@link #invoke(Object, String)}。
     *
     * @return 返回值转成 String；无合适方法或调用异常时返回 null
     */
    public static String invokeWithSingleStringArg(Object tool, String input) {
        if (tool == null) return null;
        Method m = findSingleStringToolMethod(tool.getClass());
        if (m == null) return null;
        try {
            m.setAccessible(true);
            Object out = m.invoke(tool, input != null ? input : "");
            return out == null ? null : out.toString();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Tool invokeWithSingleStringArg failed, tool={}", tool != null ? tool.getClass().getSimpleName() : "null", e);
            }
            return null;
        }
    }

    /**
     * 查找类上第一个带 @Tool 的方法（任意参数个数）。
     */
    static Method findToolMethod(Class<?> clazz) {
        for (Method m : clazz.getMethods()) {
            if (m.getAnnotation(Tool.class) != null) return m;
        }
        return null;
    }

    static Method findSingleStringToolMethod(Class<?> clazz) {
        for (Method m : clazz.getMethods()) {
            if (m.getAnnotation(Tool.class) == null) continue;
            Class<?>[] params = m.getParameterTypes();
            if (params.length != 1 || !params[0].isAssignableFrom(String.class)) continue;
            return m;
        }
        return null;
    }

    /**
     * 根据方法签名和 input 解析出参数数组。单 String 参时整段 input 作为该参数；多参或非 String 单参时从 JSON 按 key 绑定。
     */
    static Object[] resolveArgs(Method method, String input) {
        Parameter[] params = method.getParameters();
        if (params.length == 0) return new Object[0];
        if (params.length == 1 && params[0].getType() == String.class) {
            return new Object[]{ input != null ? input : "" };
        }
        // 多参或单参非 String：按 JSON 绑定
        JsonNode node = parseJson(input);
        if (node == null || !node.isObject()) return null;
        List<String> keys = jsonObjectKeys(node);
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Object val = getParamValue(node, params[i], i, keys);
            if (val == null && params[i].getType().isPrimitive()) {
                val = defaultForPrimitive(params[i].getType());
            }
            args[i] = val;
        }
        return args;
    }

    private static JsonNode parseJson(String input) {
        if (input == null || !input.trim().startsWith("{")) return null;
        try {
            return JSON.readTree(input);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> jsonObjectKeys(JsonNode node) {
        List<String> keys = new ArrayList<>();
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) keys.add(it.next());
        return keys;
    }

    /** 从 JSON 中取当前参数对应的值：先按 @P 或参数名匹配 key，再按顺序用 keys.get(i) 兜底。 */
    private static Object getParamValue(JsonNode node, Parameter param, int index, List<String> keys) {
        String[] tryKeys = paramKeys(param, index, keys);
        for (String key : tryKeys) {
            if (key == null || key.isEmpty()) continue;
            JsonNode v = node.get(key);
            if (v == null) v = node.get(key.trim());
            if (v != null && !v.isNull()) {
                return convertTo(v, param.getType());
            }
        }
        return null;
    }

    /** 当前参数在 JSON 中可用的 key：@P 值、Java 参数名、按顺序的 key。 */
    private static String[] paramKeys(Parameter param, int index, List<String> keys) {
        String pName = param.getName();
        String pDesc = null;
        if (param.getAnnotation(P.class) != null) {
            pDesc = param.getAnnotation(P.class).value();
            if (pDesc != null) pDesc = pDesc.trim();
        }
        // 优先：@P 描述（LLM 常按描述填 key）、再 Java 参数名、再按顺序
        List<String> k = new ArrayList<>();
        if (pDesc != null && !pDesc.isEmpty()) k.add(pDesc);
        if (pName != null && !pName.startsWith("arg")) k.add(pName);
        if (index < keys.size()) k.add(keys.get(index));
        return k.toArray(new String[0]);
    }

    private static Object convertTo(JsonNode v, Class<?> target) {
        if (v == null || v.isNull()) return null;
        if (target == String.class) return v.isTextual() ? v.asText() : v.toString();
        if (target == int.class || target == Integer.class) return v.asInt(0);
        if (target == long.class || target == Long.class) return v.asLong(0L);
        if (target == boolean.class || target == Boolean.class) return v.asBoolean(false);
        if (target == double.class || target == Double.class) return v.asDouble(0.0);
        if (target == float.class || target == Float.class) return (float) v.asDouble(0.0);
        return v.isTextual() ? v.asText() : v.toString();
    }

    private static Object defaultForPrimitive(Class<?> c) {
        if (c == int.class) return 0;
        if (c == long.class) return 0L;
        if (c == boolean.class) return false;
        if (c == double.class) return 0.0;
        if (c == float.class) return 0f;
        return null;
    }
}

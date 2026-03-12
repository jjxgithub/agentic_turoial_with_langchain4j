package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import dev.langchain4j.agent.tool.Tool;

import java.lang.reflect.Method;

/**
 * 通过反射调用带 {@link Tool} 注解、单 String 参数的方法，供 {@link GroupTool} 等通用执行器使用。
 */
public final class ToolInvoker {

    private ToolInvoker() {}

    /**
     * 在 tool 实例上查找「带 @Tool、单 String 参数、返回 String 或可转 String」的方法并调用。
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
            return null;
        }
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
}

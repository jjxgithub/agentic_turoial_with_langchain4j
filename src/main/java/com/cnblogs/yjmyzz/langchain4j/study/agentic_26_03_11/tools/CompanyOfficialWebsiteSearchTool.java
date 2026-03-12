package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 根据公司名称查找官网（Demo 为模拟实现，可后续接入百度搜索 API 等）。
 * 返回 JSON：{"companyName":"...","officialUrl":"...","note":"..."}，未找到时 officialUrl 为空或 note 说明。
 */
@Component("companyOfficialWebsiteSearchTool311")
public class CompanyOfficialWebsiteSearchTool {

    @Tool("根据公司名称在百度等搜索引擎上查找该公司官网。输入：公司全称或常用简称。返回 JSON：{\"companyName\":\"...\",\"officialUrl\":\"...\",\"note\":\"...\"}；未找到时 officialUrl 为空。")
    public String searchOfficialWebsite(@P("公司名称，如：华为、阿里巴巴、腾讯") String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return "{\"companyName\":\"\",\"officialUrl\":\"\",\"note\":\"未提供公司名称\"}";
        }
        String name = companyName.trim();
        // Demo：常见公司返回示例官网，其余返回占位说明；实际可接入百度搜索 API 或爬虫解析首条结果
        String url = switch (name) {
            case "华为", "华为技术" -> "https://www.huawei.com";
            case "阿里巴巴", "阿里" -> "https://www.alibaba.com";
            case "腾讯", "腾讯科技" -> "https://www.tencent.com";
            case "百度" -> "https://www.baidu.com";
            case "字节跳动", "字节" -> "https://www.bytedance.com";
            case "京东" -> "https://www.jd.com";
            case "小米", "小米科技" -> "https://www.mi.com";
            default -> "";
        };
        String note = url.isEmpty()
                ? "Demo 模式未接入真实百度 API，仅内置少数示例公司；接入 API 后可根据搜索结果解析官网链接。"
                : "Demo 模式返回内置示例链接，真实环境可替换为百度/必应等搜索 API 结果。";
        return String.format("{\"companyName\":\"%s\",\"officialUrl\":\"%s\",\"note\":\"%s\"}",
                escapeJson(name), url, escapeJson(note));
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}

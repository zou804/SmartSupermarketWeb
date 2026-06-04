package services;

import utils.ConfigManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AIService {
    private static final String API_URL = ConfigManager.getProperty("deepseek.api.url", "https://api.deepseek.com/v1/chat/completions");
    private static final String API_MODEL = ConfigManager.getProperty("deepseek.api.model", "deepseek-chat");
    private static final double TEMPERATURE = Double.parseDouble(ConfigManager.getProperty("deepseek.api.temperature", "0.7"));
    private static final int MAX_TOKENS = Integer.parseInt(ConfigManager.getProperty("deepseek.api.max_tokens", "3000"));
    private static final int TIMEOUT = Integer.parseInt(ConfigManager.getProperty("deepseek.api.timeout", "60000"));

    public static String analyzeSalesData(double totalSales, int orderCount, double avgOrderValue, 
                                         double memberOrderRatio, int memberCount, String salesTrend) {
        String apiKey = ConfigManager.getProperty("deepseek.api.key");
        
        // 如果没有配置API密钥，返回模拟分析
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_DEEPSEEK_API_KEY_HERE")) {
            return generateMockAnalysis(totalSales, orderCount, avgOrderValue, memberOrderRatio, memberCount, salesTrend);
        }

        String prompt = buildAnalysisPrompt(totalSales, orderCount, avgOrderValue, memberOrderRatio, memberCount, salesTrend);
        
        try {
            String result = callDeepSeekAPI(apiKey, prompt);
            if (result != null && !result.isEmpty() && !result.contains("API Error") && !result.contains("解析失败")) {
                return formatAnalysisResult(result);
            }
        } catch (Exception e) {
            System.err.println("DeepSeek API调用失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        // API调用失败时返回模拟分析
        return generateMockAnalysis(totalSales, orderCount, avgOrderValue, memberOrderRatio, memberCount, salesTrend);
    }

    private static String buildAnalysisPrompt(double totalSales, int orderCount, double avgOrderValue,
                                              double memberOrderRatio, int memberCount, String salesTrend) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位资深的超市经营数据分析专家，精通零售业运营分析和商业智能。\n\n");
        prompt.append("请根据以下超市销售数据，提供一份专业、详细的智能分析报告：\n\n");
        prompt.append("========== 数据概览 ==========\n");
        prompt.append(String.format("• 总销售额：¥%.2f\n", totalSales));
        prompt.append(String.format("• 订单总数：%d 单\n", orderCount));
        prompt.append(String.format("• 平均客单价：¥%.2f\n", avgOrderValue));
        prompt.append(String.format("• 会员订单占比：%.1f%%\n", memberOrderRatio));
        prompt.append(String.format("• 注册会员数：%d 人\n", memberCount));
        
        if (salesTrend != null && !salesTrend.isEmpty()) {
            prompt.append("\n========== 销售趋势 ==========\n");
            prompt.append(salesTrend + "\n");
        }
        
        prompt.append("\n========== 分析要求 ==========\n");
        prompt.append("请输出一份结构化的分析报告，包含以下7个部分：\n");
        prompt.append("1. 📈 销售数据分析 - 详细解读各项指标，分析数据背后的含义\n");
        prompt.append("2. ✨ 经营亮点 - 识别表现优秀的方面和成功因素\n");
        prompt.append("3. ⚠️ 问题诊断 - 发现潜在问题、风险点和改进空间\n");
        prompt.append("4. 💡 改进建议 - 提供具体、可执行的优化方案\n");
        prompt.append("5. 🎯 营销策略 - 针对会员和普通顾客的营销活动建议\n");
        prompt.append("6. 📉 趋势预测 - 基于现有数据的短期趋势预测\n");
        prompt.append("7. 📝 总结 - 综合分析结论和行动建议\n");
        
        prompt.append("\n请使用中文输出，格式清晰美观，数据准确，建议具体可行。");
        prompt.append("使用 emoji 图标增强可读性，避免使用Markdown格式。");
        
        return prompt.toString();
    }

    private static String callDeepSeekAPI(String apiKey, String prompt) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);

        // 构建正确的JSON请求体
        String jsonBody = buildJsonRequestBody(prompt);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                return parseDeepSeekResponse(response.toString());
            }
        } else {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
                String errorMsg = "API Error " + responseCode + ": " + errorResponse.toString();
                System.err.println(errorMsg);
                throw new Exception(errorMsg);
            }
        }
    }

    private static String buildJsonRequestBody(String prompt) {
        // 正确构建JSON，避免手动转义错误
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\": \"").append(escapeJson(API_MODEL)).append("\",");
        json.append("\"messages\": [{");
        json.append("\"role\": \"user\",");
        json.append("\"content\": \"").append(escapeJson(prompt)).append("\"");
        json.append("}],");
        json.append("\"temperature\": ").append(TEMPERATURE).append(",");
        json.append("\"max_tokens\": ").append(MAX_TOKENS);
        json.append("}");
        return json.toString();
    }

    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String parseDeepSeekResponse(String jsonResponse) {
        // 查找 content 字段的值
        String marker = "\"content\":\"";
        int contentStart = jsonResponse.indexOf(marker);
        if (contentStart == -1) {
            return "API响应解析失败：未找到content字段";
        }
        contentStart += marker.length();
        
        // 查找 content 字段的结束位置（处理转义的引号）
        int contentEnd = contentStart;
        boolean inEscape = false;
        while (contentEnd < jsonResponse.length()) {
            char c = jsonResponse.charAt(contentEnd);
            if (inEscape) {
                inEscape = false;
            } else if (c == '\\') {
                inEscape = true;
            } else if (c == '"') {
                break;
            }
            contentEnd++;
        }
        
        if (contentEnd >= jsonResponse.length()) {
            // 如果找不到结束引号，取到最后
            contentEnd = jsonResponse.length();
        }
        
        String content = jsonResponse.substring(contentStart, contentEnd);
        // 解码转义字符
        content = content.replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
        return content;
    }

    private static String formatAnalysisResult(String rawResult) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("📊 [DEEPSEEK_API] DeepSeek AI 智能分析报告\n\n");
        formatted.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        formatted.append(rawResult);
        formatted.append("\n\n");
        formatted.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        formatted.append("© 2026 DeepSeek AI 智能分析系统\n");
        formatted.append("分析时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return formatted.toString();
    }

    private static String generateMockAnalysis(double totalSales, int orderCount, double avgOrderValue,
                                               double memberOrderRatio, int memberCount, String salesTrend) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("📊 智能超市销售分析报告（本地模式）\n\n");
        analysis.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        // 1. 销售数据分析
        analysis.append("📈 一、销售数据分析\n");
        analysis.append("────────────────────────────────────────────────\n");
        analysis.append(String.format("• 总销售额: ¥%.2f\n", totalSales));
        analysis.append(String.format("• 订单总数: %d 单\n", orderCount));
        analysis.append(String.format("• 平均客单价: ¥%.2f\n", avgOrderValue));
        analysis.append(String.format("• 会员订单占比: %.1f%%\n", memberOrderRatio));
        analysis.append(String.format("• 注册会员数: %d 人\n", memberCount));
        analysis.append("\n📊 数据解读：\n");
        analysis.append("  - 本周期共产生 ").append(orderCount).append(" 笔订单，");
        analysis.append("平均每单消费 ¥").append(String.format("%.2f", avgOrderValue)).append("\n");
        analysis.append("  - 会员消费占比 ").append(String.format("%.1f", memberOrderRatio)).append("%，");
        analysis.append("会员体系发挥一定作用\n");
        analysis.append("\n");
        
        // 2. 经营亮点
        analysis.append("✨ 二、经营亮点\n");
        analysis.append("────────────────────────────────────────────────\n");
        if (avgOrderValue >= 50) {
            analysis.append("✓ 客单价表现优秀，顾客消费能力较强\n");
        }
        if (memberOrderRatio >= 35) {
            analysis.append("✓ 会员活跃度良好，会员忠诚度较高\n");
        }
        if (orderCount >= 200) {
            analysis.append("✓ 订单数量充足，店铺流量稳定\n");
        }
        if (totalSales >= 15000) {
            analysis.append("✓ 销售额达到预期目标\n");
        }
        analysis.append("\n");
        
        // 3. 问题诊断
        analysis.append("⚠️ 三、问题诊断\n");
        analysis.append("────────────────────────────────────────────────\n");
        if (memberOrderRatio < 35) {
            analysis.append("✗ 会员转化率偏低，需要加强会员营销力度\n");
        }
        if (avgOrderValue < 50) {
            analysis.append("✗ 客单价偏低，可考虑增加高毛利商品或促销组合\n");
        }
        if (orderCount < 200) {
            analysis.append("✗ 订单数量不足，需要提升店铺曝光度\n");
        }
        analysis.append("\n");
        
        // 4. 改进建议
        analysis.append("💡 四、改进建议\n");
        analysis.append("────────────────────────────────────────────────\n");
        analysis.append("1. 优化商品陈列，将高毛利商品放置在显眼位置\n");
        analysis.append("2. 增加促销活动，如满减、折扣、买一送一等\n");
        analysis.append("3. 完善会员权益体系，提高会员粘性\n");
        analysis.append("4. 利用数据分析优化库存管理，避免缺货\n");
        analysis.append("5. 定期开展会员专属活动，提升会员活跃度\n");
        analysis.append("\n");
        
        // 5. 营销策略
        analysis.append("🎯 五、营销策略建议\n");
        analysis.append("────────────────────────────────────────────────\n");
        analysis.append("• 会员日活动：每月8/18/28日会员享8.8折优惠\n");
        analysis.append("• 满减活动：满200减30，满500减80，满1000减200\n");
        analysis.append("• 积分计划：消费1元积1分，积分可兑换礼品或优惠券\n");
        analysis.append("• 新会员专享：注册即送20元优惠券，首单立减10元\n");
        analysis.append("• 周末促销：每周六日推出特价商品，吸引顾客到店\n");
        analysis.append("\n");
        
        // 6. 销售趋势
        analysis.append("📉 六、销售趋势分析\n");
        analysis.append("────────────────────────────────────────────────\n");
        if (salesTrend != null && !salesTrend.isEmpty()) {
            analysis.append("  " + salesTrend + "\n");
        } else {
            analysis.append("  数据不足，无法分析趋势\n");
        }
        analysis.append("\n");
        
        // 7. 总结
        analysis.append("📝 七、总结\n");
        analysis.append("────────────────────────────────────────────────\n");
        analysis.append("综合分析，当前店铺运营状况良好，但仍有提升空间。\n");
        analysis.append("建议重点关注会员体系建设和客单价提升，\n");
        analysis.append("通过多样化的营销策略吸引更多顾客，提高销售额。\n");
        analysis.append("\n");
        
        analysis.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        analysis.append("© 2026 DeepSeek AI 智能分析系统\n");
        analysis.append("分析时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return analysis.toString();
    }
}

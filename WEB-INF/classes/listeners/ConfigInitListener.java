package listeners;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import utils.ConfigManager;

@WebListener
public class ConfigInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        ConfigManager.init(context);
        
        if (ConfigManager.isDeepSeekConfigured()) {
            context.log("✅ DeepSeek API 已配置，将使用真实AI分析");
        } else {
            context.log("⚠️ DeepSeek API 未配置，将使用模拟分析模式");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // 清理资源（如果需要）
    }
}

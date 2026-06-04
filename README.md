# 🏪 Smart Supermarket Management System

智能超市管理系统 - 基于 Java Servlet + Tomcat 的超市销售数据分析平台

## ✨ 功能特性

- 📊 **数据仪表盘** - 实时展示销售数据和关键指标
- 🤖 **AI智能分析** - 集成 DeepSeek AI 进行销售趋势分析和经营建议
- 🛒 **商品管理** - 商品列表、添加、编辑、详情查看
- 👥 **会员管理** - 会员列表、积分管理
- 💳 **销售结账** - 购物车功能、会员折扣、订单结算
- 📈 **销售统计** - 销售记录、趋势图表

## 🛠️ 技术栈

- **前端**: HTML5 + CSS3 + JavaScript (ES6+)
- **后端**: Java Servlet (JDK 21)
- **服务器**: Apache Tomcat 11.0
- **AI服务**: DeepSeek API

## 📁 项目结构

```
WebContent/
├── index.html              # 主页面
├── css/
│   └── style.css          # 样式文件
├── js/
│   └── app.js             # 前端逻辑
└── WEB-INF/
    ├── web.xml            # Web配置
    ├── config.properties  # 应用配置
    └── classes/
        ├── servlets/      # Servlet类
        ├── models/        # 数据模型
        ├── services/      # 业务服务
        ├── listeners/     # 监听器
        └── utils/         # 工具类
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Apache Tomcat 11.0+

### 部署步骤

1. **克隆项目**
```bash
git clone https://github.com/zou804/SmartSupermarketWeb.git
```

2. **编译项目**
```bash
cd SmartSupermarketWeb
javac -cp "path/to/tomcat/lib/servlet-api.jar" -d WEB-INF/classes WEB-INF/classes/**/*.java
```

3. **部署到 Tomcat**
   - 将 `WebContent` 目录复制到 Tomcat 的 `webapps` 目录
   - 启动 Tomcat 服务器

4. **访问应用**
```
http://localhost:8080/WebContent/
```

## 📋 API 接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/products` | GET | 获取商品列表 |
| `/api/products/{id}` | GET | 获取商品详情 |
| `/api/products` | POST | 添加商品 |
| `/api/products/{id}` | PUT | 更新商品 |
| `/api/products/identify` | POST | AI识别商品类目 |
| `/api/members` | GET | 获取会员列表 |
| `/api/members` | POST | 添加会员 |
| `/api/sales` | GET | 获取销售记录 |
| `/api/sales` | POST | 结账 |
| `/api/analysis/ai` | GET | AI分析报告 |

## 📝 配置说明

编辑 `WEB-INF/config.properties` 文件：

```properties
# DeepSeek API 配置
deepseek.api.url=https://api.deepseek.com/v1/chat/completions
deepseek.api.key=your_api_key_here
deepseek.model=deepseek-chat
```

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**Author**: zou804  
**GitHub**: https://github.com/zou804/SmartSupermarketWeb
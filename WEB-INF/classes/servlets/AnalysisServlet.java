package servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

@WebServlet("/api/*")
public class AnalysisServlet extends HttpServlet {

    private static final ConcurrentHashMap<Integer, Member> members = new ConcurrentHashMap<>();
    private static final List<Sale> sales = new ArrayList<>();
    private static final ConcurrentHashMap<Integer, models.Product> products = new ConcurrentHashMap<>();
    private static final AtomicInteger memberIdCounter = new AtomicInteger(1);
    private static final AtomicInteger saleIdCounter = new AtomicInteger(1);
    private static final AtomicInteger productIdCounter = new AtomicInteger(1);

    // 商品类别
    private static final String[] PRODUCT_CATEGORIES = {"食品", "饮料", "日用品", "生鲜", "零食"};
    
    // 商品名称（按类别组织）
    private static final String[] FOOD_NAMES = {"牛奶", "面包", "鸡蛋", "大米", "食用油", "面粉", "酱油", "醋", "盐", "糖", "火腿肠", "方便面", "饼干", "豆腐"};
    private static final String[] DRINK_NAMES = {"可乐", "雪碧", "矿泉水", "果汁", "咖啡", "茶", "啤酒", "红酒", "酸奶", "牛奶"};
    private static final String[] SNACK_NAMES = {"薯片", "饼干", "糖果", "巧克力", "坚果", "果冻", "薯片", "饼干", "糖果", "巧克力"};
    private static final String[] FRESH_NAMES = {"苹果", "香蕉", "橙子", "葡萄", "西瓜", "草莓", "蓝莓", "桃子", "梨", "芒果", "猪肉", "牛肉", "鸡肉", "鱼肉", "虾"};
    private static final String[] DAILY_NAMES = {"毛巾", "牙刷", "牙膏", "洗发水", "沐浴露", "洗衣液", "肥皂", "纸巾", "垃圾袋", "洗洁精"};

    // 会员姓名
    private static final String[] FIRST_NAMES = {"张", "李", "王", "刘", "陈", "杨", "赵", "黄", "周", "吴"};
    private static final String[] LAST_NAMES = {"伟", "强", "芳", "娜", "敏", "静", "磊", "丽", "军", "洋"};

    static {
        initializeMockData();
    }

    private static void initializeMockData() {
        Random random = new Random(42);
        
        // 创建50个会员
        for (int i = 1; i <= 50; i++) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String name = firstName + lastName;
            String phone = "1" + (random.nextInt(8) + 3) + String.format("%09d", i);
            members.put(i, new Member(i, name, phone));
        }
        memberIdCounter.set(51);

        // 创建100个商品
        for (int i = 1; i <= 100; i++) {
            String category = PRODUCT_CATEGORIES[i % 5]; // 循环使用5个类别，保证均匀分布
            String name;
            String[] nameArray;
            
            // 根据类别选择对应的商品名称数组
            switch (category) {
                case "食品":
                    nameArray = FOOD_NAMES;
                    break;
                case "饮料":
                    nameArray = DRINK_NAMES;
                    break;
                case "零食":
                    nameArray = SNACK_NAMES;
                    break;
                case "生鲜":
                    nameArray = FRESH_NAMES;
                    break;
                case "日用品":
                    nameArray = DAILY_NAMES;
                    break;
                default:
                    nameArray = FOOD_NAMES;
            }
            
            name = nameArray[random.nextInt(nameArray.length)] + (i % 10 == 0 ? "(" + (i / 10) + ")" : "");
            
            // 根据类别设置合理的价格范围
            double cost, price;
            if ("饮料".equals(category)) {
                cost = Math.round(random.nextDouble() * 8 + 1) * 1.0;        // 1-9元成本 (可乐、矿泉水等)
                price = Math.round(cost * (random.nextDouble() * 0.25 + 1.15) * 100) / 100.0;  // 15%-40%利润
            } else if ("零食".equals(category)) {
                cost = Math.round(random.nextDouble() * 10 + 2) * 1.0;       // 2-12元成本 (薯片、糖果等)
                price = Math.round(cost * (random.nextDouble() * 0.3 + 1.15) * 100) / 100.0;   // 15%-45%利润
            } else if ("食品".equals(category)) {
                cost = Math.round(random.nextDouble() * 20 + 3) * 1.0;       // 3-23元成本 (方便面、面包等)
                price = Math.round(cost * (random.nextDouble() * 0.3 + 1.15) * 100) / 100.0;   // 15%-45%利润
            } else if ("生鲜".equals(category)) {
                cost = Math.round(random.nextDouble() * 30 + 3) * 1.0;       // 3-33元成本 (水果、肉类等)
                price = Math.round(cost * (random.nextDouble() * 0.35 + 1.12) * 100) / 100.0; // 12%-47%利润
            } else { // 日用品
                cost = Math.round(random.nextDouble() * 15 + 3) * 1.0;       // 3-18元成本 (牙膏、毛巾等)
                price = Math.round(cost * (random.nextDouble() * 0.35 + 1.15) * 100) / 100.0; // 15%-50%利润
            }
            
            int stock = random.nextInt(200) + 10;
            String barcode = String.format("%013d", i * 1234567);
            
            products.put(i, new models.Product(i, name, category, price, cost, stock, barcode));
        }
        productIdCounter.set(101);

        // 创建300条销售记录（最近30天）
        LocalDateTime now = LocalDateTime.now();
        int saleId = 1;
        
        // 合并所有商品名称数组用于销售记录
        String[] allProductNames = new String[FOOD_NAMES.length + DRINK_NAMES.length + 
                                               SNACK_NAMES.length + FRESH_NAMES.length + DAILY_NAMES.length];
        System.arraycopy(FOOD_NAMES, 0, allProductNames, 0, FOOD_NAMES.length);
        System.arraycopy(DRINK_NAMES, 0, allProductNames, FOOD_NAMES.length, DRINK_NAMES.length);
        System.arraycopy(SNACK_NAMES, 0, allProductNames, FOOD_NAMES.length + DRINK_NAMES.length, SNACK_NAMES.length);
        System.arraycopy(FRESH_NAMES, 0, allProductNames, FOOD_NAMES.length + DRINK_NAMES.length + SNACK_NAMES.length, FRESH_NAMES.length);
        System.arraycopy(DAILY_NAMES, 0, allProductNames, FOOD_NAMES.length + DRINK_NAMES.length + SNACK_NAMES.length + FRESH_NAMES.length, DAILY_NAMES.length);
        
        for (int day = 29; day >= 0; day--) {
            LocalDate date = LocalDate.now().minusDays(day);
            // 每天生成5-15条销售记录
            int salesPerDay = random.nextInt(11) + 5;
            
            for (int i = 0; i < salesPerDay; i++) {
                LocalDateTime saleTime = date.atTime(random.nextInt(24), random.nextInt(60), random.nextInt(60));
                Integer memberId = random.nextDouble() < 0.4 ? random.nextInt(50) + 1 : null; // 40%会员订单
                String productName = allProductNames[random.nextInt(allProductNames.length)];
                int quantity = random.nextInt(5) + 1;
                double price = Math.round((random.nextDouble() * 50 + 5) * 100) / 100.0; // 5-55元
                double originalAmount = price * quantity;
                double discountedAmount = memberId != null ? Math.round(originalAmount * 0.9 * 100) / 100.0 : originalAmount;
                
                sales.add(new Sale(
                    saleId++,
                    memberId,
                    productName,
                    quantity,
                    originalAmount,
                    discountedAmount,
                    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    saleTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                ));
            }
        }
        saleIdCounter.set(saleId);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            out.println("{\"error\": \"Invalid API endpoint\"}");
            return;
        }

        String[] parts = pathInfo.split("/");
        String endpoint = parts[1];

        switch (endpoint) {
            case "sales":
                handleSalesGet(request, response, out);
                break;
            case "members":
                handleMembersGet(request, response, out);
                break;
            case "analysis":
                handleAnalysisGet(request, response, out);
                break;
            case "products":
                handleProductsGet(request, response, out);
                break;
            default:
                out.println("{\"error\": \"Endpoint not found\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            out.println("{\"error\": \"Invalid API endpoint\"}");
            return;
        }

        String[] parts = pathInfo.split("/");
        String endpoint = parts[1];

        switch (endpoint) {
            case "members":
                handleMembersPost(request, response, out);
                break;
            case "sales":
                handleSalesPost(request, response, out);
                break;
            case "products":
                if (parts.length > 2 && "identify".equals(parts[2])) {
                    handleProductIdentify(request, response, out);
                } else {
                    handleProductsPost(request, response, out);
                }
                break;
            default:
                out.println("{\"error\": \"Endpoint not found\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            out.println("{\"error\": \"Invalid API endpoint\"}");
            return;
        }

        String[] parts = pathInfo.split("/");
        String endpoint = parts[1];

        switch (endpoint) {
            case "products":
                handleProductsPut(request, response, out);
                break;
            default:
                out.println("{\"error\": \"Endpoint not found\"}");
        }
    }

    private void handleProductsGet(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String pathInfo = request.getPathInfo();
        String[] parts = pathInfo.split("/");

        if (parts.length > 2) {
            try {
                int productId = Integer.parseInt(parts[2]);
                models.Product product = products.get(productId);
                if (product != null) {
                    out.println("{\"id\":" + product.getId() + 
                        ",\"name\":\"" + product.getName() + "\"" +
                        ",\"category\":\"" + product.getCategory() + "\"" +
                        ",\"price\":" + product.getPrice() +
                        ",\"cost\":" + product.getCost() +
                        ",\"stock\":" + product.getStock() +
                        ",\"barcode\":\"" + product.getBarcode() + "\"" +
                        ",\"profit\":" + String.format("%.2f", product.getProfit()) +
                        ",\"profitMargin\":" + String.format("%.1f", product.getProfitMargin()) + "}");
                } else {
                    out.println("{\"error\": \"Product not found\"}");
                }
            } catch (NumberFormatException e) {
                out.println("{\"error\": \"Invalid product ID\"}");
            }
        } else {
            String category = request.getParameter("category");
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (models.Product product : products.values()) {
                if (category != null && !category.isEmpty() && !product.getCategory().equals(category)) {
                    continue;
                }
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"id\":" + product.getId() + 
                    ",\"name\":\"" + product.getName() + "\"" +
                    ",\"category\":\"" + product.getCategory() + "\"" +
                    ",\"price\":" + product.getPrice() +
                    ",\"cost\":" + product.getCost() +
                    ",\"stock\":" + product.getStock() +
                    ",\"barcode\":\"" + product.getBarcode() + "\"}");
            }
            sb.append("]");
            out.println(sb.toString());
        }
    }

    private void handleProductsPost(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            String data = sb.toString();

            String name = extractValue(data, "name");
            String category = extractValue(data, "category");
            String priceStr = extractValue(data, "price");
            String stockStr = extractValue(data, "stock");

            if (name.isEmpty() || category.isEmpty() || priceStr.isEmpty()) {
                out.println("{\"success\":false,\"error\":\"请填写完整信息\"}");
                return;
            }

            double price = Double.parseDouble(priceStr);
            double cost = price * 0.7; // 假设成本为售价的70%
            int stock = stockStr.isEmpty() ? 100 : Integer.parseInt(stockStr);
            String barcode = String.format("%013d", System.currentTimeMillis() % 10000000000000L);

            int newId = productIdCounter.getAndIncrement();
            products.put(newId, new models.Product(newId, name, category, price, cost, stock, barcode));

            out.println("{\"success\":true,\"id\":" + newId + ",\"message\":\"商品添加成功\"}");
        } catch (Exception e) {
            out.println("{\"success\":false,\"error\":\"添加失败: " + e.getMessage() + "\"}");
        }
    }

    private void handleProductsPut(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        try {
            String pathInfo = request.getPathInfo();
            String[] parts = pathInfo.split("/");
            
            if (parts.length < 3) {
                out.println("{\"success\":false,\"error\":\"缺少商品ID\"}");
                return;
            }
            
            int productId = Integer.parseInt(parts[2]);
            models.Product product = products.get(productId);
            
            if (product == null) {
                out.println("{\"success\":false,\"error\":\"商品不存在\"}");
                return;
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            String data = sb.toString();

            String name = extractValue(data, "name");
            String category = extractValue(data, "category");
            String priceStr = extractValue(data, "price");
            String costStr = extractValue(data, "cost");
            String stockStr = extractValue(data, "stock");
            String barcode = extractValue(data, "barcode");

            if (!name.isEmpty()) product.setName(name);
            if (!category.isEmpty()) product.setCategory(category);
            if (!priceStr.isEmpty()) product.setPrice(Double.parseDouble(priceStr));
            if (!costStr.isEmpty()) product.setCost(Double.parseDouble(costStr));
            if (!stockStr.isEmpty()) product.setStock(Integer.parseInt(stockStr));
            if (!barcode.isEmpty()) product.setBarcode(barcode);

            out.println("{\"success\":true,\"message\":\"商品更新成功\"}");
        } catch (Exception e) {
            out.println("{\"success\":false,\"error\":\"更新失败: " + e.getMessage() + "\"}");
        }
    }

    private void handleProductIdentify(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            String data = sb.toString();

            String productName = extractValue(data, "name");
            
            if (productName == null || productName.isEmpty()) {
                out.println("{\"success\":false,\"error\":\"请提供商品名称\"}");
                return;
            }

            // 使用本地逻辑先识别，节省API调用
            String category = null;
            double referencePrice = 0.0;
            
            // 简单的本地识别规则
            if (productName.matches(".*(可乐|雪碧|饮料|水|茶|啤酒|咖啡).*")) {
                category = "饮料";
                referencePrice = productName.contains("可乐") ? 3.5 : 2.5;
            } else if (productName.matches(".*(薯片|饼干|糖果|零食|坚果|巧克力).*")) {
                category = "零食";
                referencePrice = 8.5;
            } else if (productName.matches(".*(面包|方便面|火腿|香肠|罐头|食品).*")) {
                category = "食品";
                referencePrice = 5.5;
            } else if (productName.matches(".*(苹果|香蕉|橙子|西瓜|草莓|葡萄|肉类|鱼|蛋|生鲜).*")) {
                category = "生鲜";
                referencePrice = 12.5;
            } else if (productName.matches(".*(牙膏|牙刷|香皂|毛巾|洗衣液|洗发水|洗洁精|日用品).*")) {
                category = "日用品";
                referencePrice = 9.5;
            } else {
                // 尝试使用DeepSeek进行智能识别
                try {
                    String prompt = "请识别商品的类目和参考单价。商品名称：" + productName + 
                        "\n\n返回JSON格式，格式如下：\n" + 
                        "{\"category\":\"类目\",\"referencePrice\":参考价格}\n" + 
                        "\n类目只能是以下五种之一：食品、饮料、日用品、生鲜、零食。" + 
                        "价格单位是人民币元。";
                    
                    String aiResult = services.AIService.analyzeSalesData(0, 0, 0, 0, 0, prompt);
                    
                    // 尝试从AI返回中提取JSON
                    String jsonStr = aiResult.substring(aiResult.indexOf("{"), aiResult.lastIndexOf("}") + 1);
                    String aiCategory = extractValue(jsonStr, "category");
                    String priceStr = extractValue(jsonStr, "referencePrice");
                    
                    if (!aiCategory.isEmpty()) {
                        category = aiCategory;
                    }
                    if (!priceStr.isEmpty()) {
                        referencePrice = Double.parseDouble(priceStr);
                    }
                } catch (Exception e) {
                    // AI调用失败，使用默认值
                    category = "食品";
                    referencePrice = 5.0;
                }
            }
            
            // 确保category是合法值
            String[] validCategories = {"食品", "饮料", "日用品", "生鲜", "零食"};
            boolean isValidCategory = false;
            for (String cat : validCategories) {
                if (cat.equals(category)) {
                    isValidCategory = true;
                    break;
                }
            }
            if (!isValidCategory) {
                category = "食品";
            }

            out.println("{\"success\":true,\"category\":\"" + category + "\",\"referencePrice\":" + referencePrice + "}");
        } catch (Exception e) {
            e.printStackTrace();
            out.println("{\"success\":false,\"error\":\"识别失败: " + e.getMessage() + "\"}");
        }
    }

    private void handleSalesGet(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String pathInfo = request.getPathInfo();
        String date = request.getParameter("date");
        String action = request.getParameter("action");

        // 检查是否是 summary 请求（通过路径 /sales/summary 或查询参数 action=summary）
        boolean isSummary = (pathInfo != null && pathInfo.equals("/sales/summary")) || 
                           (action != null && action.equals("summary"));

        if (isSummary) {
            double totalAmount = 0;
            int totalSales = sales.size();
            int totalItems = 0;
            for (Sale sale : sales) {
                totalAmount += sale.getDiscountedAmount();
                totalItems += sale.getItemCount();
            }
            int memberCount = members.size();

            out.println("{\"totalAmount\":" + totalAmount + ",\"totalSales\":" + totalSales + ",\"totalItems\":" + totalItems + ",\"memberCount\":" + memberCount + "}");
        } else {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Sale sale : sales) {
                if (date != null && !date.isEmpty() && !sale.getDate().equals(date)) {
                    continue;
                }
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"id\":").append(sale.getId())
                  .append(",\"memberId\":").append(sale.getMemberId() == null ? "null" : sale.getMemberId())
                  .append(",\"date\":\"").append(sale.getDate()).append("\"")
                  .append(",\"dateTime\":\"").append(sale.getDateTime()).append("\"")
                  .append(",\"itemCount\":").append(sale.getItemCount())
                  .append(",\"discountedAmount\":").append(sale.getDiscountedAmount()).append("}");
            }
            sb.append("]");
            out.println(sb.toString());
        }
    }

    private void handleMembersGet(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String pathInfo = request.getPathInfo();
        String[] parts = pathInfo.split("/");

        if (parts.length > 2) {
            try {
                int memberId = Integer.parseInt(parts[2]);
                Member member = members.get(memberId);
                if (member != null) {
                    out.println("{\"id\":" + member.getId() + ",\"name\":\"" + member.getName() + "\",\"phone\":\"" + member.getPhone() + "\"}");
                } else {
                    out.println("{}");
                }
            } catch (NumberFormatException e) {
                out.println("{}");
            }
        } else {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Member member : members.values()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"id\":" + member.getId() + ",\"name\":\"" + member.getName() + "\",\"phone\":\"" + member.getPhone() + "\"}");
            }
            sb.append("]");
            out.println(sb.toString());
        }
    }

    private void handleAnalysisGet(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        
        String pathInfo = request.getPathInfo();
        String[] parts = pathInfo.split("/");

        if (parts.length > 2 && parts[2].equals("ai")) {
            double totalSales = 0;
            int orderCount = sales.size();
            int memberOrders = 0;
            for (Sale sale : sales) {
                totalSales += sale.getDiscountedAmount();
                if (sale.getMemberId() != null) memberOrders++;
            }
            double avgOrderValue = orderCount > 0 ? totalSales / orderCount : 0;
            double memberOrderRatio = orderCount > 0 ? (memberOrders * 100.0 / orderCount) : 0;
            int memberCount = members.size();

            // 构建销售趋势信息
            String salesTrend = buildSalesTrend();

            // 调用 AI 服务进行分析（支持真实 DeepSeek API 调用）
            String analysis = services.AIService.analyzeSalesData(totalSales, orderCount, avgOrderValue, memberOrderRatio, memberCount, salesTrend);

            String escapedAnalysis = analysis.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            out.println("{\"analysis\":\"" + escapedAnalysis + "\"}");
        } else {
            out.println("{\"error\": \"Analysis endpoint not found\"}");
        }
    }

    private String buildSalesTrend() {
        if (sales.size() < 2) {
            return "数据不足，无法分析趋势";
        }

        ConcurrentHashMap<String, Double> dailySales = new ConcurrentHashMap<>();
        
        for (Sale sale : sales) {
            String date = sale.getDate();
            dailySales.put(date, dailySales.getOrDefault(date, 0.0) + sale.getDiscountedAmount());
        }

        List<String> dates = new ArrayList<>(dailySales.keySet());
        dates.sort(String::compareTo);
        
        StringBuilder trend = new StringBuilder();
        
        if (dates.size() >= 7) {
            List<String> recentDates = dates.subList(Math.max(0, dates.size() - 7), dates.size());
            
            trend.append("近7日销售趋势：");
            for (int i = 0; i < recentDates.size(); i++) {
                if (i > 0) trend.append(" | ");
                String date = recentDates.get(i);
                trend.append(date.substring(5)).append(": ¥").append(String.format("%.0f", dailySales.get(date)));
            }
        } else {
            trend.append("销售数据较少，建议积累更多数据后分析");
        }

        return trend.toString();
    }

    private void handleMembersPost(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            String data = sb.toString();
            
            String name = extractValue(data, "name");
            String phone = extractValue(data, "phone");

            if (name.isEmpty() || phone.isEmpty()) {
                out.println("{\"success\":false,\"error\":\"请填写完整信息\"}");
                return;
            }

            int newId = memberIdCounter.getAndIncrement();
            members.put(newId, new Member(newId, name, phone));

            out.println("{\"success\":true,\"id\":" + newId + "}");
        } catch (Exception e) {
            out.println("{\"success\":false,\"error\":\"注册失败\"}");
        }
    }

    private void handleSalesPost(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            String data = sb.toString();

            String memberIdStr = extractValue(data, "memberId");
            Integer memberId = null;
            if (!memberIdStr.isEmpty() && !memberIdStr.equals("null")) {
                memberId = Integer.parseInt(memberIdStr);
            }

            // 改进的JSON解析
            int productsStart = data.indexOf("\"products\":");
            if (productsStart == -1) {
                out.println("{\"success\":false,\"error\":\"请添加商品\"}");
                return;
            }
            productsStart += 11;
            
            int bracketCount = 0;
            int productsEnd = -1;
            boolean inQuotes = false;
            
            for (int i = productsStart; i < data.length(); i++) {
                char c = data.charAt(i);
                if (c == '\"') {
                    inQuotes = !inQuotes;
                } else if (!inQuotes) {
                    if (c == '[') {
                        bracketCount++;
                    } else if (c == ']') {
                        bracketCount--;
                        if (bracketCount == 0) {
                            productsEnd = i;
                            break;
                        }
                    }
                }
            }
            
            if (productsEnd == -1) {
                out.println("{\"success\":false,\"error\":\"请添加商品\"}");
                return;
            }
            
            String productsStr = data.substring(productsStart, productsEnd + 1);
            productsStr = productsStr.substring(productsStr.indexOf('['), productsStr.lastIndexOf(']') + 1);
            
            // 解析商品数组
            double totalAmount = 0;
            int itemCount = 0;
            StringBuilder itemsDesc = new StringBuilder();
            
            int productCount = 0;
            int currentPos = 1;
            while (currentPos < productsStr.length()) {
                int objStart = productsStr.indexOf('{', currentPos);
                if (objStart == -1) break;
                int objEnd = -1;
                bracketCount = 0;
                inQuotes = false;
                for (int i = objStart; i < productsStr.length(); i++) {
                    char c = productsStr.charAt(i);
                    if (c == '\"') {
                        inQuotes = !inQuotes;
                    } else if (!inQuotes) {
                        if (c == '{') {
                            bracketCount++;
                        } else if (c == '}') {
                            bracketCount--;
                            if (bracketCount == 0) {
                                objEnd = i;
                                break;
                            }
                        }
                    }
                }
                if (objEnd == -1) break;
                
                String productJson = productsStr.substring(objStart, objEnd + 1);
                String name = extractValue(productJson, "name");
                double price = Double.parseDouble(extractValue(productJson, "price"));
                int quantity = Integer.parseInt(extractValue(productJson, "quantity"));
                
                totalAmount += price * quantity;
                itemCount += quantity;
                if (productCount > 0) itemsDesc.append(", ");
                itemsDesc.append(name).append("x").append(quantity);
                
                productCount++;
                currentPos = objEnd + 1;
            }
            
            if (productCount == 0) {
                out.println("{\"success\":false,\"error\":\"请添加商品\"}");
                return;
            }

            double discountedAmount = memberId != null ? Math.round(totalAmount * 0.9 * 100) / 100.0 : totalAmount;

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            sales.add(new Sale(saleIdCounter.getAndIncrement(), memberId, itemsDesc.toString(), itemCount, totalAmount, discountedAmount, today, now));

            out.println("{\"success\":true,\"amount\":" + discountedAmount + "}");
        } catch (Exception e) {
            e.printStackTrace();
            out.println("{\"success\":false,\"error\":\"结账失败: " + e.getMessage() + "\"}");
        }
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            if (end == -1) return "";
            return json.substring(start, end);
        } else {
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            if (end == -1) end = json.indexOf("]", start);
            if (end == -1) return "";
            return json.substring(start, end).trim();
        }
    }

    static class Member {
        private final int id;
        private final String name;
        private final String phone;

        public Member(int id, String name, String phone) {
            this.id = id;
            this.name = name;
            this.phone = phone;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getPhone() { return phone; }
    }

    static class Sale {
        private final int id;
        private final Integer memberId;
        private final String items;
        private final int itemCount;
        private final double originalAmount;
        private final double discountedAmount;
        private final String date;
        private final String dateTime;

        public Sale(int id, Integer memberId, String items, int itemCount, double originalAmount, double discountedAmount, String date, String dateTime) {
            this.id = id;
            this.memberId = memberId;
            this.items = items;
            this.itemCount = itemCount;
            this.originalAmount = originalAmount;
            this.discountedAmount = discountedAmount;
            this.date = date;
            this.dateTime = dateTime;
        }

        public int getId() { return id; }
        public Integer getMemberId() { return memberId; }
        public String getItems() { return items; }
        public int getItemCount() { return itemCount; }
        public double getOriginalAmount() { return originalAmount; }
        public double getDiscountedAmount() { return discountedAmount; }
        public String getDate() { return date; }
        public String getDateTime() { return dateTime; }
    }
}

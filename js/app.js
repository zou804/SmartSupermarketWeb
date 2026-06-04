// Global state
let cart = [];
let currentMember = null;

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    initNavigation();
    loadDashboard();
    loadMembers();
    loadProducts();
    loadProductSelector();
    initForms();
});

// Navigation
function initNavigation() {
    const navBtns = document.querySelectorAll('.nav-btn');
    const sections = document.querySelectorAll('.section');

    navBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const sectionId = this.dataset.section;

            navBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            sections.forEach(s => s.classList.remove('active'));
            document.getElementById(sectionId).classList.add('active');

            // Load data for each section
            switch(sectionId) {
                case 'dashboard':
                    loadDashboard();
                    break;
                case 'products':
                    loadProducts();
                    break;
                case 'member':
                    loadMembers();
                    break;
                case 'sale':
                    resetSaleForm();
                    break;
                case 'query':
                    queryTodaySales();
                    break;
            }
        });
    });
}

// Dashboard
async function loadDashboard() {
    try {
        const response = await fetch('/WebContent/api/sales/summary');
        const data = await response.json();

        document.getElementById('totalAmount').textContent = '¥' + data.totalAmount.toFixed(2);
        document.getElementById('totalSales').textContent = data.totalSales;
        document.getElementById('totalItems').textContent = data.totalItems;
        document.getElementById('memberCount').textContent = data.memberCount;

        // Load recent sales
        const salesResponse = await fetch('/WebContent/api/sales');
        const sales = await salesResponse.json();
        displayRecentOrders(sales);
        displayChart(sales);
    } catch (error) {
        console.error('加载数据失败:', error);
    }
}

function displayRecentOrders(sales) {
    const container = document.getElementById('recentOrdersList');
    const recent = sales.slice(-10).reverse();

    if (recent.length === 0) {
        container.innerHTML = '<p style="text-align:center;color:#999;padding:20px;">暂无订单记录</p>';
        return;
    }

    container.innerHTML = recent.map(sale => `
        <div class="order-item">
            <div class="order-info">
                <span class="order-date">${sale.dateTime}</span>
                <span class="order-items">${sale.itemCount}件商品</span>
            </div>
            <span class="order-amount">¥${sale.discountedAmount.toFixed(2)}</span>
        </div>
    `).join('');
}

function displayChart(sales) {
    const container = document.getElementById('salesChart');
    const dailyData = {};

    sales.forEach(sale => {
        const date = sale.date;
        if (!dailyData[date]) {
            dailyData[date] = 0;
        }
        dailyData[date] += sale.discountedAmount;
    });

    const dates = Object.keys(dailyData).slice(-7);
    const maxAmount = Math.max(...Object.values(dailyData).slice(-7), 1);

    if (dates.length === 0) {
        container.innerHTML = '<p style="text-align:center;color:#999;width:100%;">暂无数据</p>';
        return;
    }

    container.innerHTML = dates.map(date => {
        const amount = dailyData[date];
        const height = (amount / maxAmount * 150) + 20;
        return `<div class="chart-bar" style="height:${height}px" data-date="${date}"></div>`;
    }).join('');
}

// Product Management
async function loadProducts() {
    try {
        const category = document.getElementById('productFilter')?.value || '';
        const url = category ? `/WebContent/api/products?category=${encodeURIComponent(category)}` : '/WebContent/api/products';
        const response = await fetch(url);
        const products = await response.json();
        displayProducts(products);
    } catch (error) {
        console.error('加载商品失败:', error);
    }
}

function displayProducts(products) {
    const container = document.getElementById('productList');

    if (!products || products.length === 0) {
        container.innerHTML = '<p style="text-align:center;color:#999;padding:20px;">暂无商品记录</p>';
        return;
    }

    container.innerHTML = products.map(product => `
        <div class="product-item">
            <div class="product-info">
                <span class="product-id">ID: ${product.id}</span>
                <span class="product-name">${product.name}</span>
                <span class="product-category">📦 ${product.category}</span>
            </div>
            <div class="product-details">
                <span class="product-price">¥${product.price.toFixed(2)}</span>
                <span class="product-stock">库存: ${product.stock}</span>
                <button class="btn-detail" onclick="openProductDetail(${product.id})">详情</button>
            </div>
        </div>
    `).join('');
}

let currentProductId = null;

async function openProductDetail(productId) {
    try {
        const response = await fetch(`/WebContent/api/products/${productId}`);
        const product = await response.json();
        
        if (!product.id) {
            showToast('商品不存在', true);
            return;
        }

        currentProductId = productId;
        
        document.getElementById('detailId').textContent = product.id;
        document.getElementById('detailName').textContent = product.name;
        document.getElementById('detailCategory').textContent = product.category;
        document.getElementById('detailPrice').textContent = '¥' + product.price.toFixed(2);
        document.getElementById('detailCost').textContent = '¥' + product.cost.toFixed(2);
        document.getElementById('detailProfit').textContent = '¥' + (product.profit || (product.price - product.cost)).toFixed(2);
        document.getElementById('detailProfitMargin').textContent = (product.profitMargin || ((product.price - product.cost) / product.price * 100)).toFixed(1) + '%';
        document.getElementById('detailStock').textContent = product.stock + ' 件';
        document.getElementById('detailBarcode').textContent = product.barcode;

        document.getElementById('editProductName').value = product.name;
        document.getElementById('editProductCategory').value = product.category;
        document.getElementById('editProductPrice').value = product.price;
        document.getElementById('editProductCost').value = product.cost;
        document.getElementById('editProductStock').value = product.stock;
        document.getElementById('editProductBarcode').value = product.barcode;

        document.getElementById('productModal').style.display = 'block';
        document.querySelector('.edit-section').style.display = 'none';
        document.querySelector('.product-detail').style.display = 'block';
        document.querySelector('.modal-footer .btn-primary').style.display = 'none';
        document.querySelector('.modal-footer .btn-secondary:first-child').textContent = '编辑';
    } catch (error) {
        console.error('获取商品详情失败:', error);
        showToast('获取商品详情失败', true);
    }
}

function closeProductModal() {
    document.getElementById('productModal').style.display = 'none';
    currentProductId = null;
}

function toggleEditMode() {
    const detailSection = document.querySelector('.product-detail');
    const editSection = document.querySelector('.edit-section');
    const saveBtn = document.querySelector('.modal-footer .btn-primary');
    const editBtn = document.querySelector('.modal-footer .btn-secondary:first-child');

    if (detailSection.style.display !== 'none') {
        detailSection.style.display = 'none';
        editSection.style.display = 'block';
        saveBtn.style.display = 'inline-block';
        editBtn.textContent = '取消';
    } else {
        detailSection.style.display = 'block';
        editSection.style.display = 'none';
        saveBtn.style.display = 'none';
        editBtn.textContent = '编辑';
    }
}

async function saveProductEdit() {
    if (!currentProductId) return;

    const data = {
        name: document.getElementById('editProductName').value,
        category: document.getElementById('editProductCategory').value,
        price: document.getElementById('editProductPrice').value,
        cost: document.getElementById('editProductCost').value,
        stock: document.getElementById('editProductStock').value,
        barcode: document.getElementById('editProductBarcode').value
    };

    try {
        const response = await fetch(`/WebContent/api/products/${currentProductId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        const result = await response.json();

        if (result.success) {
            showToast('商品更新成功！');
            closeProductModal();
            loadProducts();
        } else {
            showToast('更新失败: ' + result.error, true);
        }
    } catch (error) {
        console.error('更新商品失败:', error);
        showToast('更新失败', true);
    }
}

// 点击弹窗外部关闭
document.getElementById('productModal').addEventListener('click', function(e) {
    if (e.target === this) {
        closeProductModal();
    }
});

// Member Management
async function loadMembers() {
    try {
        const response = await fetch('/WebContent/api/members');
        const members = await response.json();
        displayMembers(members);
    } catch (error) {
        console.error('加载会员失败:', error);
    }
}

function displayMembers(members) {
    const container = document.getElementById('memberList');

    if (members.length === 0) {
        container.innerHTML = '<p style="text-align:center;color:#999;padding:20px;">暂无会员记录</p>';
        return;
    }

    container.innerHTML = members.map(member => `
        <div class="member-item">
            <div class="member-info">
                <span class="member-id">ID: ${member.id}</span>
                <span class="member-name">${member.name}</span>
                <span class="member-phone">📱 ${member.phone}</span>
            </div>
            <span class="member-badge">⭐ VIP会员</span>
        </div>
    `).join('');
}

function initForms() {
    // Product form
    document.getElementById('productForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const name = document.getElementById('productNameAdd').value;
        const category = document.getElementById('productCategory').value;
        const price = parseFloat(document.getElementById('productPriceAdd').value);
        const stock = parseInt(document.getElementById('productStock').value) || 100;

        try {
            const response = await fetch('/WebContent/api/products', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ name, category, price, stock })
            });

            const result = await response.json();

            if (result.success) {
                showToast('商品添加成功！');
                document.getElementById('productForm').reset();
                loadProducts();
            } else {
                showToast('添加失败: ' + result.error, true);
            }
        } catch (error) {
            console.error('添加商品失败:', error);
            showToast('添加失败', true);
        }
    });

    // Member form
    document.getElementById('memberForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const name = document.getElementById('memberName').value;
        const phone = document.getElementById('memberPhone').value;

        try {
            const response = await fetch('/WebContent/api/members', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ name, phone })
            });

            const result = await response.json();

            if (result.success) {
                showToast('会员注册成功！');
                document.getElementById('memberForm').reset();
                loadMembers();
                loadDashboard();
            } else {
                showToast('注册失败', true);
            }
        } catch (error) {
            console.error('注册会员失败:', error);
            showToast('注册失败', true);
        }
    });
}

// Sale Functions
function checkMember() {
    const memberId = document.getElementById('saleMemberId').value.trim();
    const status = document.getElementById('memberStatus');

    if (!memberId) {
        status.textContent = '';
        status.className = '';
        currentMember = null;
        updateCartTotal();
        return;
    }

    fetch(`/WebContent/api/members/${memberId}`)
        .then(response => response.json())
        .then(member => {
            if (member.id) {
                status.textContent = `✓ 会员: ${member.name}`;
                status.className = 'valid';
                currentMember = member;
            } else {
                status.textContent = '✗ 会员不存在';
                status.className = 'invalid';
                currentMember = null;
            }
            updateCartTotal();
        })
        .catch(error => {
            status.textContent = '✗ 查询失败';
            status.className = 'invalid';
            currentMember = null;
        });
}

let allProducts = [];

async function identifyProduct() {
    const name = document.getElementById('productNameAdd').value;
    if (!name) {
        showToast('请输入商品名称', true);
        return;
    }

    const identifyBtn = document.getElementById('identifyBtn');
    identifyBtn.disabled = true;
    identifyBtn.textContent = '🤖 识别中...';

    try {
        const response = await fetch('/WebContent/api/products/identify', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ name: name })
        });

        const result = await response.json();

        if (result.success) {
            document.getElementById('productCategory').value = result.category;
            document.getElementById('productPriceAdd').value = result.referencePrice.toFixed(2);
            showToast('识别成功！');
        } else {
            showToast(result.error || '识别失败', true);
        }
    } catch (error) {
        console.error('识别失败:', error);
        showToast('识别失败', true);
    } finally {
        identifyBtn.disabled = false;
        identifyBtn.textContent = '🤖 AI识别';
    }
}

async function loadProductSelector() {
    try {
        const response = await fetch('/WebContent/api/products');
        allProducts = await response.json();
        
        const selector = document.getElementById('productSelector');
        selector.innerHTML = '<option value="">-- 请选择商品 --</option>';
        
        allProducts.forEach(product => {
            const option = document.createElement('option');
            option.value = product.id;
            option.textContent = `${product.name} - ¥${product.price.toFixed(2)}`;
            selector.appendChild(option);
        });
    } catch (error) {
        console.error('加载商品列表失败:', error);
    }
}

function searchProducts() {
    const searchTerm = document.getElementById('productSearch').value.toLowerCase();
    const selector = document.getElementById('productSelector');
    
    selector.innerHTML = '<option value="">-- 请选择商品 --</option>';
    
    const filtered = allProducts.filter(p => 
        p.name.toLowerCase().includes(searchTerm) ||
        p.category.toLowerCase().includes(searchTerm)
    );
    
    filtered.forEach(product => {
        const option = document.createElement('option');
        option.value = product.id;
        option.textContent = `${product.name} - ¥${product.price.toFixed(2)}`;
        selector.appendChild(option);
    });
}

function onProductSelect() {
    const productId = document.getElementById('productSelector').value;
    if (productId) {
        const product = allProducts.find(p => p.id == productId);
        if (product) {
            showToast(`已选择: ${product.name} ¥${product.price.toFixed(2)}`, false);
        }
    }
}

function addToCart() {
    const productId = document.getElementById('productSelector').value;
    const quantity = parseInt(document.getElementById('productQuantity').value);

    if (!productId || isNaN(quantity) || quantity < 1) {
        showToast('请选择商品并输入数量', true);
        return;
    }

    const product = allProducts.find(p => p.id == productId);
    if (!product) {
        showToast('商品不存在', true);
        return;
    }

    const existingItem = cart.find(item => item.id == productId);
    if (existingItem) {
        existingItem.quantity += quantity;
    } else {
        cart.push({ id: product.id, name: product.name, price: product.price, quantity });
    }

    document.getElementById('productQuantity').value = '1';

    displayCart();
}

function displayCart() {
    const container = document.getElementById('cartItems');

    if (cart.length === 0) {
        container.innerHTML = '<p style="text-align:center;color:#999;padding:20px;">购物车为空</p>';
        return;
    }

    container.innerHTML = cart.map((item, index) => `
        <div class="cart-item">
            <div class="cart-item-info">
                <span>${item.name}</span>
                <span>x${item.quantity}</span>
            </div>
            <div>
                <span class="cart-item-price">¥${(item.price * item.quantity).toFixed(2)}</span>
                <button onclick="removeFromCart(${index})" style="margin-left:10px;background:#dc3545;color:white;border:none;padding:5px 10px;border-radius:5px;cursor:pointer;">删除</button>
            </div>
        </div>
    `).join('');

    updateCartTotal();
}

function removeFromCart(index) {
    cart.splice(index, 1);
    displayCart();
}

function updateCartTotal() {
    const originalTotal = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    document.getElementById('originalTotal').textContent = '¥' + originalTotal.toFixed(2);

    const discountRow = document.getElementById('discountRow');
    const discountAmount = document.getElementById('discountAmount');
    const finalTotal = document.getElementById('finalTotal');

    if (currentMember) {
        const discount = originalTotal * 0.1;
        discountRow.style.display = 'flex';
        discountAmount.textContent = '-¥' + discount.toFixed(2);
        finalTotal.textContent = '¥' + (originalTotal - discount).toFixed(2);
    } else {
        discountRow.style.display = 'none';
        finalTotal.textContent = '¥' + originalTotal.toFixed(2);
    }
}

async function processCheckout() {
    if (cart.length === 0) {
        showToast('购物车为空', true);
        return;
    }

    const saleData = {
        memberId: currentMember ? currentMember.id : null,
        products: cart
    };

    try {
        const response = await fetch('/WebContent/api/sales', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(saleData)
        });

        const result = await response.json();

        if (result.success) {
            showToast('结账成功！');
            resetSaleForm();
            loadDashboard();
        } else {
            showToast('结账失败', true);
        }
    } catch (error) {
        console.error('结账失败:', error);
        showToast('结账失败', true);
    }
}

function resetSaleForm() {
    cart = [];
    currentMember = null;
    document.getElementById('saleMemberId').value = '';
    document.getElementById('memberStatus').textContent = '';
    document.getElementById('memberStatus').className = '';
    displayCart();
}

// Query Functions
async function queryTodaySales() {
    const today = new Date().toISOString().split('T')[0];
    await queryBySpecificDate(today);
}

async function queryAllSales() {
    try {
        const response = await fetch('/WebContent/api/sales');
        const sales = await response.json();
        displayQueryResults(sales);
    } catch (error) {
        console.error('查询失败:', error);
    }
}

async function queryByDate() {
    const date = document.getElementById('queryDate').value;
    if (!date) {
        showToast('请选择日期', true);
        return;
    }
    await queryBySpecificDate(date);
}

async function queryBySpecificDate(date) {
    try {
        const response = await fetch(`/WebContent/api/sales?date=${date}`);
        const sales = await response.json();
        displayQueryResults(sales);
    } catch (error) {
        console.error('查询失败:', error);
    }
}

function displayQueryResults(sales) {
    const summaryContainer = document.getElementById('querySummary');
    const listContainer = document.getElementById('queryList');

    const totalAmount = sales.reduce((sum, s) => sum + s.discountedAmount, 0);
    const totalItems = sales.reduce((sum, s) => sum + s.itemCount, 0);

    summaryContainer.innerHTML = `
        <div class="summary-item">
            <h4>${sales.length}</h4>
            <p>订单数</p>
        </div>
        <div class="summary-item">
            <h4>¥${totalAmount.toFixed(2)}</h4>
            <p>销售总额</p>
        </div>
        <div class="summary-item">
            <h4>${totalItems}</h4>
            <p>商品件数</p>
        </div>
    `;

    if (sales.length === 0) {
        listContainer.innerHTML = '<p style="text-align:center;color:#999;padding:40px;">暂无数据</p>';
        return;
    }

    listContainer.innerHTML = sales.reverse().map(sale => `
        <div class="order-item" style="margin-top:15px;">
            <div class="order-info">
                <span class="order-date">${sale.dateTime}</span>
                <span class="order-items">${sale.itemCount}件商品</span>
                ${sale.memberId ? '<span style="color:#667eea;">会员</span>' : ''}
            </div>
            <span class="order-amount">¥${sale.discountedAmount.toFixed(2)}</span>
        </div>
    `).join('');
}

// AI Analysis
async function performAIAnalysis() {
    const resultsContainer = document.getElementById('aiResults');
    const statusElement = document.getElementById('apiStatus');
    
    resultsContainer.innerHTML = '<div class="loading"><span class="loading-text">🤖 AI正在分析数据...</span><span class="loading-spinner"></span></div>';
    if (statusElement) {
        statusElement.innerHTML = '<span class="status-text">连接中...</span>';
        statusElement.className = 'api-status loading';
    }

    try {
        const startTime = Date.now();
        const response = await fetch('/WebContent/api/analysis/ai');
        const result = await response.json();
        const responseTime = (Date.now() - startTime) / 1000;

        if (result.error) {
            resultsContainer.innerHTML = `<p style="color:#dc3545;">❌ ${result.error}</p>`;
            if (statusElement) {
                statusElement.innerHTML = '⚠️ 连接失败，使用本地分析';
                statusElement.className = 'api-status local';
            }
        } else {
            // 美化分析结果显示
            const isRealAI = result.analysis.includes('[DEEPSEEK_API]');
            resultsContainer.innerHTML = `<div class="analysis-content">${formatAnalysis(result.analysis)}</div>`;
            
            if (statusElement) {
                if (isRealAI) {
                    statusElement.innerHTML = `✅ DeepSeek AI 已连接 | 响应时间: ${responseTime.toFixed(1)}s`;
                    statusElement.className = 'api-status connected';
                } else {
                    statusElement.innerHTML = '📊 本地分析模式';
                    statusElement.className = 'api-status local';
                }
            }
        }
    } catch (error) {
        console.error('AI分析失败:', error);
        resultsContainer.innerHTML = '<p style="color:#dc3545;">❌ 分析失败，请检查网络连接或稍后重试</p>';
        if (statusElement) {
            statusElement.innerHTML = '⚠️ 连接失败，使用本地分析';
            statusElement.className = 'api-status local';
        }
    }
}

function formatAnalysis(text) {
    // 替换标题格式
    text = text.replace(/📊\s*(.+)/g, '<h2>📊 $1</h2>');
    text = text.replace(/📈\s*一、(.+)/g, '<h3>📈 一、$1</h3>');
    text = text.replace(/✨\s*二、(.+)/g, '<h3>✨ 二、$1</h3>');
    text = text.replace(/⚠️\s*三、(.+)/g, '<h3>⚠️ 三、$1</h3>');
    text = text.replace(/💡\s*四、(.+)/g, '<h3>💡 四、$1</h3>');
    text = text.replace(/🎯\s*五、(.+)/g, '<h3>🎯 五、$1</h3>');
    text = text.replace(/📉\s*六、(.+)/g, '<h3>📉 六、$1</h3>');
    text = text.replace(/📝\s*七、(.+)/g, '<h3>📝 七、$1</h3>');
    
    // 替换列表项
    text = text.replace(/^•\s(.+)$/gm, '<p>• $1</p>');
    text = text.replace(/^✓\s(.+)$/gm, '<p class="success">✓ $1</p>');
    text = text.replace(/^✗\s(.+)$/gm, '<p class="error">✗ $1</p>');
    
    // 替换分隔线
    text = text.replace(/━━━━+/g, '<hr style="border:none;border-top:2px dashed #e0e0e0;margin:20px 0;">');
    
    // 处理段落
    text = text.replace(/\n\n/g, '</p><p>');
    text = '<p>' + text + '</p>';
    
    // 移除多余的空段落
    text = text.replace(/<p>\s*<\/p>/g, '');
    
    return text;
}

// Toast Notification
function showToast(message, isError = false) {
    const toast = document.createElement('div');
    toast.className = 'toast' + (isError ? ' error' : '');
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, 3000);
}

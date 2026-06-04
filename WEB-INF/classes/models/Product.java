package models;

public class Product {
    private int id;
    private String name;
    private String category;
    private double price;
    private double cost;
    private int stock;
    private String barcode;

    public Product() {}

    public Product(int id, String name, String category, double price, double cost, int stock, String barcode) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.cost = cost;
        this.stock = stock;
        this.barcode = barcode;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public double getProfit() { return price - cost; }
    public double getProfitMargin() { return cost > 0 ? (price - cost) / price * 100 : 0; }
}

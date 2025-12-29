package com.example.telegrambot;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс для представления корзины покупок
 */
public class Cart {
    private Map<String, Integer> items; // productId -> quantity
    
    public Cart() {
        this.items = new HashMap<>();
    }
    
    /**
     * Добавить товар в корзину
     */
    public void addItem(String productId, int quantity) {
        items.put(productId, items.getOrDefault(productId, 0) + quantity);
    }
    
    /**
     * Удалить товар из корзины
     */
    public void removeItem(String productId) {
        items.remove(productId);
    }
    
    /**
     * Изменить количество товара в корзине
     */
    public void updateQuantity(String productId, int quantity) {
        if (quantity <= 0) {
            removeItem(productId);
        } else {
            items.put(productId, quantity);
        }
    }
    
    /**
     * Очистить корзину
     */
    public void clear() {
        items.clear();
    }
    
    /**
     * Получить количество товара в корзине
     */
    public int getQuantity(String productId) {
        return items.getOrDefault(productId, 0);
    }
    
    /**
     * Проверить, пуста ли корзина
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    /**
     * Получить все товары в корзине
     */
    public Map<String, Integer> getItems() {
        return new HashMap<>(items);
    }
    
    /**
     * Получить общее количество товаров в корзине
     */
    public int getTotalItems() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    @Override
    public String toString() {
        return "Cart{" +
                "items=" + items +
                '}';
    }
}

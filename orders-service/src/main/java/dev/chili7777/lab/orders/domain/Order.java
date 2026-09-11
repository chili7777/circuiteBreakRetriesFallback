package dev.chili7777.lab.orders.domain;
import java.math.BigDecimal;
public record Order(String id, BigDecimal amount, String status, String detail) {}

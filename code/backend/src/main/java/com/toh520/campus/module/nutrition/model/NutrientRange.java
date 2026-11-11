package com.toh520.campus.module.nutrition.model;

/** 用户明确提出的单餐营养上下界；null 表示该方向未限制，仍叠加身体能量范围。 */
public record NutrientRange(Double min, Double max) {
  public boolean accepts(double value) {
    return (min == null || value >= min) && (max == null || value <= max);
  }
}

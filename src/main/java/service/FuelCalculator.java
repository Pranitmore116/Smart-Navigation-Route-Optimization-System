package service;

public class FuelCalculator {
    public record FuelResult(double requiredLitres, double cost) {}

    public FuelResult calculate(double distanceKm, double mileageKmPerLitre, double pricePerLitre) {
        if (distanceKm < 0) throw new IllegalArgumentException("Distance cannot be negative");
        if (mileageKmPerLitre <= 0) throw new IllegalArgumentException("Mileage must be greater than zero");
        if (pricePerLitre < 0) throw new IllegalArgumentException("Fuel price cannot be negative");
        double litres = distanceKm / mileageKmPerLitre;
        return new FuelResult(litres, litres * pricePerLitre);
    }
}
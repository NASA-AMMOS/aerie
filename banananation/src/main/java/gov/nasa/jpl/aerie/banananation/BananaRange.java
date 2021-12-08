package gov.nasa.jpl.aerie.banananation;

public record BananaRange<T extends Comparable<T>>(T a, T b) { }

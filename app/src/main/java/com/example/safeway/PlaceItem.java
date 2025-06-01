package com.example.safeway;

public class PlaceItem {
    private final String name;
    private final double lat;
    private final double lng;

    public PlaceItem(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public String getName() {
        return name;
    }
    public double getLat() {
        return lat;
    }
    public double getLng() {
        return lng;
    }

    // ArrayAdapter에서 toString() 리턴값을 드롭다운 목록에 표시합니다.
    @Override
    public String toString() {
        return name;
    }
}

package com.example.ogiropa;

public class LocationItem {
    private String note;
    private double lat;
    private double lng;
    public LocationItem(String note, double lat, double lng) {
        this.note = note;
        this.lat = lat;
        this.lng = lng;
    }

    public String getNote() { return note; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }

    @Override
    public String toString() {
        return "LocationItem{" +
                "note='" + note + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                '}';
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        LocationItem other = (LocationItem) obj;
        return Double.compare(other.lat, lat) == 0 &&
                Double.compare(other.lng, lng) == 0;
    }


}

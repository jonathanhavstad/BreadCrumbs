package com.mac.training.breadcrumbs;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by jonathanhavstad on 9/12/16.
 */

@Entity
public class LatLongData {

    private Long id;
    private Double lat;
    private Double lon;
    private Long time;
    private Float speed;

    @Generated(hash = 764355436)
    public LatLongData(Long id, Double lat, Double lon, Long time, Float speed) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.time = time;
        this.speed = speed;
    }

    @Generated(hash = 473682046)
    public LatLongData() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Float getSpeed() {
        return speed;
    }

    public void setSpeed(Float speed) {
        this.speed = speed;
    }
}

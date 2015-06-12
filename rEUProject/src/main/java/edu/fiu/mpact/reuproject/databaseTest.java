package edu.fiu.mpact.reuproject;

/**
 * Created by Rachelle on 6/11/15.
 */
public class databaseTest {

    private int id;
    private int datetime;
    private float mapx;
    private float mapy;
    private int rss;
    private String apName;
    private String mac;
    private int map;

    public databaseTest(int id, int datetime, float mapx, float mapy, int rss, String apName, String mac, int map) {
        this.id = id;
        this.datetime = datetime;
        this.mapx = mapx;
        this.mapy = mapy;
        this.rss = rss;
        this.apName = apName;
        this.mac = mac;
        this.map = map;
    }

    public int getId() {
        return id;
    }

    public int getDatetime() {
        return datetime;
    }

    public float getMapx() {
        return mapx;
    }

    public float getMapy() {
        return mapy;
    }

    public int getRss() {
        return rss;
    }

    public String getApName() {
        return apName;
    }

    public String getMac() {
        return mac;
    }

    public int getMap() {
        return map;
    }
}

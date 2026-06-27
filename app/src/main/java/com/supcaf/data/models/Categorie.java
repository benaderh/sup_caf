package com.supcaf.data.models;

public class Categorie {
    private long id;
    private String cat;
    private String obs;

    public Categorie() {}
    public Categorie(long id, String cat) { this.id = id; this.cat = cat; }

    public long getId()            { return id; }
    public void setId(long v)     { this.id = v; }
    public String getCat()         { return cat; }
    public void setCat(String v)  { this.cat = v; }
    public String getObs()         { return obs; }
    public void setObs(String v)  { this.obs = v; }

    @Override
    public String toString() { return cat != null ? cat : ""; }
}

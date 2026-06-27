package com.supcaf.data.models;

public class Tiers {
    private long id;
    private String tier;
    private String type;   // 'F', 'C', 'A'
    private String obs;
    // Calculs dynamiques (non stockés)
    private double solde;  // dette si F, créance si C

    public Tiers() {}

    public Tiers(long id, String tier, String type) {
        this.id   = id;
        this.tier = tier;
        this.type = type;
    }

    public long getId()              { return id; }
    public void setId(long v)       { this.id = v; }

    public String getTier()          { return tier; }
    public void setTier(String v)   { this.tier = v; }

    public String getType()          { return type; }
    public void setType(String v)   { this.type = v; }

    public String getObs()           { return obs; }
    public void setObs(String v)    { this.obs = v; }

    public double getSolde()         { return solde; }
    public void setSolde(double v)  { this.solde = v; }

    public boolean isFournisseur()   { return "F".equals(type); }
    public boolean isClient()        { return "C".equals(type); }

    /** Libellé du type pour affichage */
    public String getTypeLabel() {
        if ("F".equals(type)) return "Fournisseur";
        if ("C".equals(type)) return "Client";
        return "Autres";
    }

    @Override
    public String toString() { return tier != null ? tier : ""; }
}

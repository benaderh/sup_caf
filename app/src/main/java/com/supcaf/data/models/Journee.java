package com.supcaf.data.models;

import java.util.ArrayList;
import java.util.List;

public class Journee {
    private long id;
    private String date;
    private String type;     // A, V, VF, EC, AU
    private String libelle;
    private long idTiers;
    private double mt;       // montant brut total lignes
    private double remise;
    private double enc;      // encaissé (ventes client)
    private double dec;      // décaissé (achats fournisseur)
    // Jointure affichage
    private String nomTiers;
    private String typeTiers;
    // Lignes détail (pour saisie)
    private List<JourneeDetail> details = new ArrayList<>();

    public Journee() {}

    // ── Getters / Setters ─────────────────────────────────
    public long getId()               { return id; }
    public void setId(long v)        { this.id = v; }

    public String getDate()           { return date; }
    public void setDate(String v)    { this.date = v; }

    public String getType()           { return type; }
    public void setType(String v)    { this.type = v; }

    public String getLibelle()        { return libelle; }
    public void setLibelle(String v) { this.libelle = v; }

    public long getIdTiers()          { return idTiers; }
    public void setIdTiers(long v)   { this.idTiers = v; }

    public double getMt()             { return mt; }
    public void setMt(double v)      { this.mt = v; }

    public double getRemise()         { return remise; }
    public void setRemise(double v)  { this.remise = v; }

    public double getEnc()            { return enc; }
    public void setEnc(double v)     { this.enc = v; }

    public double getDec()            { return dec; }
    public void setDec(double v)     { this.dec = v; }

    public String getNomTiers()       { return nomTiers; }
    public void setNomTiers(String v){ this.nomTiers = v; }

    public String getTypeTiers()      { return typeTiers; }
    public void setTypeTiers(String v){ this.typeTiers = v; }

    public List<JourneeDetail> getDetails()           { return details; }
    public void setDetails(List<JourneeDetail> list) { this.details = list; }

    // ── Calculs ───────────────────────────────────────────
    /** Net à payer après remise */
    public double getNet() { return mt - remise; }

    /** Reste non réglé */
    public double getReste() {
        if ("A".equals(type) || "VF".equals(type)) return getNet() - dec;
        if ("V".equals(type) || "EC".equals(type)) return getNet() - enc;
        return 0;
    }

    /** Libellé du type pour affichage */
    public String getTypeLabel() {
        switch (type != null ? type : "") {
            case "A":  return "Achat";
            case "V":  return "Vente";
            case "VF": return "Versement Fourn.";
            case "EC": return "Encaissement";
            case "AU": return "Autres";
            default:   return type;
        }
    }

    /** Recalcule mt à partir des lignes détail */
    public void recalculerMontant() {
        double total = 0;
        for (JourneeDetail d : details) total += d.getMontantTotal();
        this.mt = total;
    }
}

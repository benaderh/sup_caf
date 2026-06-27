package com.supcaf.data.models;

public class Article {
    private long id;
    private String codeBarre;   // nullable, unique si renseigné
    private String art;         // libellé court — NOT NULL UNIQUE
    private long idCategorie;
    private String article;     // description longue
    private String ua;          // unité achat
    private String uv;          // unité vente
    private double coef;        // coefficient ua→uv
    private double pa;          // prix achat
    private double pv;          // prix vente
    private double qs;          // stock courant
    private String obs;
    // Jointure affichage
    private String nomCategorie;

    public Article() {
        this.coef = 1.0;
        this.ua = "U";
        this.uv = "U";
    }

    // ── Getters / Setters ────────────────────────────────────
    public long getId()                     { return id; }
    public void setId(long id)             { this.id = id; }

    public String getCodeBarre()           { return codeBarre; }
    public void setCodeBarre(String v)     { this.codeBarre = (v != null && v.trim().isEmpty()) ? null : v; }

    public String getArt()                 { return art; }
    public void setArt(String v)           { this.art = v; }

    public long getIdCategorie()           { return idCategorie; }
    public void setIdCategorie(long v)     { this.idCategorie = v; }

    public String getArticle()             { return article; }
    public void setArticle(String v)       { this.article = v; }

    public String getUa()                  { return ua; }
    public void setUa(String v)            { this.ua = v != null ? v : "U"; }

    public String getUv()                  { return uv; }
    public void setUv(String v)            { this.uv = v != null ? v : "U"; }

    public double getCoef()                { return coef; }
    public void setCoef(double v)          { this.coef = v <= 0 ? 1 : v; }

    public double getPa()                  { return pa; }
    public void setPa(double v)            { this.pa = v; }

    public double getPv()                  { return pv; }
    public void setPv(double v)            { this.pv = v; }

    public double getQs()                  { return qs; }
    public void setQs(double v)            { this.qs = v; }

    public String getObs()                 { return obs; }
    public void setObs(String v)           { this.obs = v; }

    public String getNomCategorie()        { return nomCategorie; }
    public void setNomCategorie(String v)  { this.nomCategorie = v; }

    /** Prix vente affiché (unité de vente) */
    public double getPvUv() { return pv; }

    /** Valeur stock = qs * pa */
    public double getValeurStock() { return qs * pa; }

    @Override
    public String toString() {
        String code = art != null ? art : "";
        String libelle = article != null ? article.trim() : "";
        return libelle.isEmpty() ? code : code + " - " + libelle;
    }
}

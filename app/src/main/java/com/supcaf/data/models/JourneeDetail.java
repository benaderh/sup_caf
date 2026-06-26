package com.supcaf.data.models;

public class JourneeDetail {
    private long id;
    private long idJournee;
    private long idArticle;
    private double quantite;
    private double prix;       // prix unitaire au moment de l'op.
    private double qsApres;   // stock restant après opération (snapshot)
    private String obs;
    // Jointure affichage
    private String nomArticle;
    private String uniteVente;

    public JourneeDetail() {}

    public JourneeDetail(long idArticle, String nomArticle, double quantite,
                         double prix, String uniteVente) {
        this.idArticle  = idArticle;
        this.nomArticle = nomArticle;
        this.quantite   = quantite;
        this.prix       = prix;
        this.uniteVente = uniteVente;
    }

    public long getId()               { return id; }
    public void setId(long v)        { this.id = v; }

    public long getIdJournee()        { return idJournee; }
    public void setIdJournee(long v) { this.idJournee = v; }

    public long getIdArticle()        { return idArticle; }
    public void setIdArticle(long v) { this.idArticle = v; }

    public double getQuantite()       { return quantite; }
    public void setQuantite(double v){ this.quantite = v; }

    public double getPrix()           { return prix; }
    public void setPrix(double v)    { this.prix = v; }

    public double getQsApres()        { return qsApres; }
    public void setQsApres(double v) { this.qsApres = v; }

    public String getObs()            { return obs; }
    public void setObs(String v)     { this.obs = v; }

    public String getNomArticle()          { return nomArticle; }
    public void setNomArticle(String v)   { this.nomArticle = v; }

    public String getUniteVente()          { return uniteVente; }
    public void setUniteVente(String v)   { this.uniteVente = v; }

    /** Montant total de la ligne */
    public double getMontantTotal() { return quantite * prix; }
}

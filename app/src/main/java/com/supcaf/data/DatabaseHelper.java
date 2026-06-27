package com.supcaf.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "datah.db";
    public static final int DB_VERSION = 2;

    // ── Tables ──────────────────────────────────────────────
    public static final String T_CATEGORIE      = "categorie";
    public static final String T_ARTICLES       = "articles";
    public static final String T_TIERS          = "tiers";
    public static final String T_JOURNEE        = "journee";
    public static final String T_JOURNEE_DETAIL = "journee_detail";

    // ── Colonnes catégorie ──────────────────────────────────
    public static final String CAT_ID  = "c_id";
    public static final String CAT_CAT = "c_cat";
    public static final String CAT_OBS = "c_obs";

    // ── Colonnes articles ───────────────────────────────────
    public static final String ART_ID      = "a_id";
    public static final String ART_CODEB   = "a_codeb";   // code barre — UNIQUE, nullable
    public static final String ART_ART     = "a_art";     // libellé court — NOT NULL UNIQUE
    public static final String ART_IDC     = "id_c";      // FK catégorie
    public static final String ART_ARTICLE = "a_article"; // description longue
    public static final String ART_UA      = "ua";        // unité achat
    public static final String ART_UV      = "uv";        // unité vente
    public static final String ART_COEF    = "a_coef";    // coefficient conversion ua→uv
    public static final String ART_PA      = "pa";        // prix achat
    public static final String ART_PV      = "pv";        // prix vente
    public static final String ART_QS      = "qs";        // stock courant
    public static final String ART_OBS     = "a_obs";

    // ── Colonnes tiers ──────────────────────────────────────
    public static final String TRS_ID   = "t_id";
    public static final String TRS_TIER = "t_tier";
    public static final String TRS_TYPE = "t_type";  // 'F','C','A'
    public static final String TRS_OBS  = "t_obs";

    // ── Colonnes journee ────────────────────────────────────
    public static final String JRN_ID      = "j_id";
    public static final String JRN_DATE    = "j_date";
    public static final String JRN_TYPE    = "j_type";    // 'A','V','VF','EC','AU'
    public static final String JRN_LIBELLE = "j_libelle";
    public static final String JRN_IDT     = "id_t";      // FK tiers
    public static final String JRN_MT      = "j_mt";      // montant brut
    public static final String JRN_REMISE  = "j_remise";
    public static final String JRN_ENC     = "j_enc";     // encaissé (client)
    public static final String JRN_DEC     = "j_dec";     // décaissé (fournisseur)

    // ── Colonnes journee_detail ─────────────────────────────
    public static final String DET_ID  = "d_id";
    public static final String DET_IDJ = "id_j";   // FK journee
    public static final String DET_IDA = "id_a";   // FK articles
    public static final String DET_Q   = "d_q";    // quantité
    public static final String DET_P   = "d_p";    // prix unitaire
    public static final String DET_QS  = "d_qs";   // stock restant après op (snapshot)
    public static final String DET_OBS = "d_obs";

    // ── Types journée ───────────────────────────────────────
    public static final String TYPE_ACHAT        = "A";
    public static final String TYPE_VENTE        = "V";
    public static final String TYPE_VERSEMENT_F  = "VF";
    public static final String TYPE_ENCAISSEMENT = "EC";
    public static final String TYPE_AUTRES       = "AU";

    // ── Types tiers ─────────────────────────────────────────
    public static final String TIERS_FOURNISSEUR = "F";
    public static final String TIERS_CLIENT      = "C";
    public static final String TIERS_AUTRES      = "A";

    // ── Tiers par défaut ────────────────────────────────────
    public static final long FOURN_COMPTANT_ID  = 1;
    public static final long CLIENT_COMPTANT_ID = 2;

    // ── Singleton ───────────────────────────────────────────
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ── Création des tables ─────────────────────────────────
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = ON;");

        db.execSQL("CREATE TABLE " + T_CATEGORIE + " ("
                + CAT_ID  + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CAT_CAT + " TEXT NOT NULL,"
                + CAT_OBS + " TEXT"
                + ");");

        db.execSQL("CREATE TABLE " + T_ARTICLES + " ("
                + ART_ID      + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ART_CODEB   + " TEXT UNIQUE,"
                + ART_ART     + " TEXT NOT NULL UNIQUE,"
                + ART_IDC     + " INTEGER,"
                + ART_ARTICLE + " TEXT,"
                + ART_UA      + " TEXT DEFAULT 'U',"
                + ART_UV      + " TEXT DEFAULT 'U',"
                + ART_COEF    + " REAL DEFAULT 1,"
                + ART_PA      + " REAL DEFAULT 0,"
                + ART_PV      + " REAL DEFAULT 0,"
                + ART_QS      + " REAL DEFAULT 0,"
                + ART_OBS     + " TEXT,"
                + "FOREIGN KEY(" + ART_IDC + ") REFERENCES " + T_CATEGORIE + "(" + CAT_ID + ")"
                + ");");

        db.execSQL("CREATE TABLE " + T_TIERS + " ("
                + TRS_ID   + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TRS_TIER + " TEXT NOT NULL,"
                + TRS_TYPE + " TEXT NOT NULL CHECK(" + TRS_TYPE + " IN ('F','C','A')),"
                + TRS_OBS  + " TEXT"
                + ");");

        db.execSQL("CREATE TABLE " + T_JOURNEE + " ("
                + JRN_ID      + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + JRN_DATE    + " TEXT NOT NULL,"
                + JRN_TYPE    + " TEXT NOT NULL CHECK(" + JRN_TYPE + " IN ('A','V','VF','EC','AU')),"
                + JRN_LIBELLE + " TEXT,"
                + JRN_IDT     + " INTEGER,"
                + JRN_MT      + " REAL DEFAULT 0,"
                + JRN_REMISE  + " REAL DEFAULT 0,"
                + JRN_ENC     + " REAL DEFAULT 0,"
                + JRN_DEC     + " REAL DEFAULT 0,"
                + "FOREIGN KEY(" + JRN_IDT + ") REFERENCES " + T_TIERS + "(" + TRS_ID + ")"
                + ");");

        db.execSQL("CREATE TABLE " + T_JOURNEE_DETAIL + " ("
                + DET_ID  + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + DET_IDJ + " INTEGER NOT NULL,"
                + DET_IDA + " INTEGER NOT NULL,"
                + DET_Q   + " REAL DEFAULT 0,"
                + DET_P   + " REAL DEFAULT 0,"
                + DET_QS  + " REAL DEFAULT 0,"
                + DET_OBS + " TEXT,"
                + "FOREIGN KEY(" + DET_IDJ + ") REFERENCES " + T_JOURNEE + "(" + JRN_ID + "),"
                + "FOREIGN KEY(" + DET_IDA + ") REFERENCES " + T_ARTICLES + "(" + ART_ID + ")"
                + ");");

        // Index utiles pour les performances
        db.execSQL("CREATE INDEX idx_art_codeb ON " + T_ARTICLES + "(" + ART_CODEB + ");");
        db.execSQL("CREATE INDEX idx_jrn_date  ON " + T_JOURNEE + "(" + JRN_DATE + ");");
        db.execSQL("CREATE INDEX idx_jrn_type  ON " + T_JOURNEE + "(" + JRN_TYPE + ");");
        db.execSQL("CREATE INDEX idx_det_idj   ON " + T_JOURNEE_DETAIL + "(" + DET_IDJ + ");");

        // Données initiales
        insertDonneesInitiales(db);
    }

    private void insertDonneesInitiales(SQLiteDatabase db) {
        // Tiers par défaut (ids 1 et 2 réservés)
        ContentValues fourn = new ContentValues();
        fourn.put(TRS_TIER, "Comptant/F");
        fourn.put(TRS_TYPE, TIERS_FOURNISSEUR);
        fourn.put(TRS_OBS, "Fournisseur par défaut — achats comptant");
        db.insert(T_TIERS, null, fourn);

        ContentValues client = new ContentValues();
        client.put(TRS_TIER, "Comptant/C");
        client.put(TRS_TYPE, TIERS_CLIENT);
        client.put(TRS_OBS, "Client par défaut — ventes comptant");
        db.insert(T_TIERS, null, client);

        ContentValues autres = new ContentValues();
        autres.put(TRS_TIER, "Comptant/A");
        autres.put(TRS_TYPE, TIERS_AUTRES);
        autres.put(TRS_OBS, "Tiers par défaut — autres opérations");
        db.insert(T_TIERS, null, autres);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Depuis v2, les categories ne sont plus alimentees par defaut.
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = ON;");
            verifierTiersParDefaut(db);
        }
    }

    private void verifierTiersParDefaut(SQLiteDatabase db) {
        // Mettre à jour les anciens noms si existants
        db.execSQL("UPDATE " + T_TIERS + " SET " + TRS_TIER + " = 'Comptant/F' WHERE " + TRS_ID + " = 1 AND " + TRS_TIER + " = 'Comptant Fournisseur'");
        db.execSQL("UPDATE " + T_TIERS + " SET " + TRS_TIER + " = 'Comptant/C' WHERE " + TRS_ID + " = 2 AND " + TRS_TIER + " = 'Comptant Client'");
        db.execSQL("UPDATE " + T_TIERS + " SET " + TRS_TIER + " = 'Comptant/A' WHERE " + TRS_ID + " = 3 AND " + TRS_TIER + " = 'Autres Comptant'");

        // Tiers 1
        try (android.database.Cursor c = db.rawQuery("SELECT 1 FROM " + T_TIERS + " WHERE " + TRS_ID + " = 1", null)) {
            if (!c.moveToFirst()) {
                ContentValues fourn = new ContentValues();
                fourn.put(TRS_ID, 1);
                fourn.put(TRS_TIER, "Comptant/F");
                fourn.put(TRS_TYPE, TIERS_FOURNISSEUR);
                fourn.put(TRS_OBS, "Fournisseur par défaut — achats comptant");
                db.insert(T_TIERS, null, fourn);
            }
        }
        // Tiers 2
        try (android.database.Cursor c = db.rawQuery("SELECT 1 FROM " + T_TIERS + " WHERE " + TRS_ID + " = 2", null)) {
            if (!c.moveToFirst()) {
                ContentValues client = new ContentValues();
                client.put(TRS_ID, 2);
                client.put(TRS_TIER, "Comptant/C");
                client.put(TRS_TYPE, TIERS_CLIENT);
                client.put(TRS_OBS, "Client par défaut — ventes comptant");
                db.insert(T_TIERS, null, client);
            }
        }
        // Tiers 3
        try (android.database.Cursor c = db.rawQuery("SELECT 1 FROM " + T_TIERS + " WHERE " + TRS_ID + " = 3", null)) {
            if (!c.moveToFirst()) {
                ContentValues autres = new ContentValues();
                autres.put(TRS_ID, 3);
                autres.put(TRS_TIER, "Comptant/A");
                autres.put(TRS_TYPE, TIERS_AUTRES);
                autres.put(TRS_OBS, "Tiers par défaut — autres opérations");
                db.insert(T_TIERS, null, autres);
            }
        }
    }
}

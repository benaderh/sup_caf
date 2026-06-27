package com.supcaf.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.models.Journee;
import com.supcaf.data.models.JourneeDetail;
import java.util.ArrayList;
import java.util.List;

public class JourneeDao {
    private final DatabaseHelper dbHelper;
    private final ArticleDao articleDao;

    public JourneeDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.articleDao = new ArticleDao(dbHelper);
    }

    // ── Mapper ───────────────────────────────────────────────
    private Journee fromCursor(Cursor c) {
        Journee j = new Journee();
        j.setId(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.JRN_ID)));
        j.setDate(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.JRN_DATE)));
        j.setType(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.JRN_TYPE)));
        int libIdx = c.getColumnIndex(DatabaseHelper.JRN_LIBELLE);
        if (libIdx >= 0) j.setLibelle(c.getString(libIdx));
        j.setIdTiers(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.JRN_IDT)));
        j.setMt(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.JRN_MT)));
        j.setRemise(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.JRN_REMISE)));
        j.setEnc(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.JRN_ENC)));
        j.setDec(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.JRN_DEC)));
        int nomTIdx = c.getColumnIndex("t_tier");
        if (nomTIdx >= 0) j.setNomTiers(c.getString(nomTIdx));
        int typeTIdx = c.getColumnIndex("t_type");
        if (typeTIdx >= 0) j.setTypeTiers(c.getString(typeTIdx));
        return j;
    }

    private JourneeDetail detailFromCursor(Cursor c) {
        JourneeDetail d = new JourneeDetail();
        d.setId(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.DET_ID)));
        d.setIdJournee(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.DET_IDJ)));
        d.setIdArticle(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.DET_IDA)));
        d.setQuantite(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.DET_Q)));
        d.setPrix(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.DET_P)));
        d.setQsApres(c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.DET_QS)));
        int obsIdx = c.getColumnIndex(DatabaseHelper.DET_OBS);
        if (obsIdx >= 0) d.setObs(c.getString(obsIdx));
        int nomAIdx = c.getColumnIndex("a_art");
        if (nomAIdx >= 0) d.setNomArticle(c.getString(nomAIdx));
        int uvIdx = c.getColumnIndex("uv");
        if (uvIdx >= 0) d.setUniteVente(c.getString(uvIdx));
        return d;
    }

    // ── Enregistrement atomique Achat ou Vente ───────────────
    /**
     * Enregistre une journée avec ses lignes détail dans une seule transaction.
     * Met à jour le stock de chaque article.
     * @param isAchat true = achat (stock augmente), false = vente (stock diminue)
     */
    public long enregistrerOperation(Journee j, boolean isAchat) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Insérer l'entête journée
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.JRN_DATE,    j.getDate());
            cv.put(DatabaseHelper.JRN_TYPE,    j.getType());
            cv.put(DatabaseHelper.JRN_LIBELLE, j.getLibelle());
            cv.put(DatabaseHelper.JRN_IDT,     j.getIdTiers());
            cv.put(DatabaseHelper.JRN_MT,      j.getMt());
            cv.put(DatabaseHelper.JRN_REMISE,  j.getRemise());
            cv.put(DatabaseHelper.JRN_ENC,     j.getEnc());
            cv.put(DatabaseHelper.JRN_DEC,     j.getDec());
            long jId = db.insert(DatabaseHelper.T_JOURNEE, null, cv);
            if (jId < 0) throw new Exception("Erreur insertion journée");

            // 2. Insérer les lignes et mettre à jour le stock
            for (JourneeDetail d : j.getDetails()) {
                // Récupérer stock actuel
                double stockActuel = 0;
                try (Cursor sc = db.rawQuery(
                        "SELECT qs FROM articles WHERE a_id=?",
                        new String[]{String.valueOf(d.getIdArticle())})) {
                    if (sc.moveToFirst()) stockActuel = sc.getDouble(0);
                }

                double nouveauStock = isAchat
                        ? stockActuel + d.getQuantite()
                        : stockActuel - d.getQuantite();
                d.setQsApres(nouveauStock);

                // Insérer la ligne détail
                ContentValues dcv = new ContentValues();
                dcv.put(DatabaseHelper.DET_IDJ, jId);
                dcv.put(DatabaseHelper.DET_IDA, d.getIdArticle());
                dcv.put(DatabaseHelper.DET_Q,   d.getQuantite());
                dcv.put(DatabaseHelper.DET_P,   d.getPrix());
                dcv.put(DatabaseHelper.DET_QS,  nouveauStock);
                dcv.put(DatabaseHelper.DET_OBS, d.getObs());
                db.insert(DatabaseHelper.T_JOURNEE_DETAIL, null, dcv);

                // Mettre à jour stock article
                articleDao.mettreAJourStock(db, d.getIdArticle(), nouveauStock);
            }

            db.setTransactionSuccessful();
            return jId;
        } catch (Exception e) {
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /** Enregistrement simple (VF, EC, Autres) sans lignes détail */
    public long enregistrerSimple(Journee j) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.JRN_DATE,    j.getDate());
        cv.put(DatabaseHelper.JRN_TYPE,    j.getType());
        cv.put(DatabaseHelper.JRN_LIBELLE, j.getLibelle());
        cv.put(DatabaseHelper.JRN_IDT,     j.getIdTiers());
        cv.put(DatabaseHelper.JRN_MT,      j.getMt());
        cv.put(DatabaseHelper.JRN_REMISE,  j.getRemise());
        cv.put(DatabaseHelper.JRN_ENC,     j.getEnc());
        cv.put(DatabaseHelper.JRN_DEC,     j.getDec());
        return db.insert(DatabaseHelper.T_JOURNEE, null, cv);
    }

    /** Supprime une journée et ses lignes (avec restauration stock) */
    public boolean supprimer(long jId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Récupérer le type pour savoir sens du stock
            String type = null;
            try (Cursor c = db.rawQuery(
                    "SELECT j_type FROM journee WHERE j_id=?",
                    new String[]{String.valueOf(jId)})) {
                if (c.moveToFirst()) type = c.getString(0);
            }
            boolean isAchat = "A".equals(type);
            boolean isVente  = "V".equals(type);

            if (isAchat || isVente) {
                // Restaurer les stocks
                try (Cursor c = db.rawQuery(
                        "SELECT id_a, d_q FROM journee_detail WHERE id_j=?",
                        new String[]{String.valueOf(jId)})) {
                    while (c.moveToNext()) {
                        long idA = c.getLong(0);
                        double q  = c.getDouble(1);
                        double stockActuel = 0;
                        try (Cursor sc = db.rawQuery(
                                "SELECT qs FROM articles WHERE a_id=?",
                                new String[]{String.valueOf(idA)})) {
                            if (sc.moveToFirst()) stockActuel = sc.getDouble(0);
                        }
                        double restaure = isAchat ? stockActuel - q : stockActuel + q;
                        articleDao.mettreAJourStock(db, idA, restaure);
                    }
                }
                db.delete(DatabaseHelper.T_JOURNEE_DETAIL,
                        DatabaseHelper.DET_IDJ + "=?",
                        new String[]{String.valueOf(jId)});
            }

            db.delete(DatabaseHelper.T_JOURNEE,
                    DatabaseHelper.JRN_ID + "=?",
                    new String[]{String.valueOf(jId)});

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // ── Requêtes de liste ────────────────────────────────────
    private static final String SQL_BASE =
        "SELECT j.*, t.t_tier, t.t_type FROM journee j " +
        "LEFT JOIN tiers t ON j.id_t = t.t_id ";

    public List<Journee> listerAchats() {
        return listerParType(DatabaseHelper.TYPE_ACHAT);
    }

    public List<Journee> listerVentes() {
        return listerParType(DatabaseHelper.TYPE_VENTE);
    }

    public List<Journee> listerParType(String type) {
        List<Journee> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                SQL_BASE + "WHERE j.j_type=? ORDER BY j.j_date DESC, j.j_id DESC",
                new String[]{type})) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    public List<Journee> listerParTiers(long tiersId) {
        List<Journee> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                SQL_BASE + "WHERE j.id_t=? ORDER BY j.j_date DESC, j.j_id DESC",
                new String[]{String.valueOf(tiersId)})) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    public List<Journee> listerTout() {
        List<Journee> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                SQL_BASE + "ORDER BY j.j_date DESC, j.j_id DESC", null)) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    public List<JourneeDetail> listerDetails(long jId) {
        List<JourneeDetail> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT d.*, a.a_art, a.uv FROM journee_detail d " +
                "JOIN articles a ON d.id_a=a.a_id WHERE d.id_j=?",
                new String[]{String.valueOf(jId)})) {
            while (c.moveToNext()) list.add(detailFromCursor(c));
        }
        return list;
    }

    public Journee parId(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SQL_BASE + "WHERE j.j_id=?",
                new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) return fromCursor(c);
        }
        return null;
    }
}

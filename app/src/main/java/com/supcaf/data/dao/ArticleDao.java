package com.supcaf.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.models.Article;
import java.util.ArrayList;
import java.util.List;

public class ArticleDao {
    private final DatabaseHelper dbHelper;

    public ArticleDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // ── Mapper Cursor → Article ──────────────────────────────
    private Article fromCursor(Cursor c) {
        Article a = new Article();
        a.setId(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.ART_ID)));
        int cbIdx = c.getColumnIndex(DatabaseHelper.ART_CODEB);
        if (cbIdx >= 0 && !c.isNull(cbIdx)) a.setCodeBarre(c.getString(cbIdx));
        a.setArt(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.ART_ART)));
        int idcIdx = c.getColumnIndex(DatabaseHelper.ART_IDC);
        if (idcIdx >= 0) a.setIdCategorie(c.getLong(idcIdx));
        int artIdx = c.getColumnIndex(DatabaseHelper.ART_ARTICLE);
        if (artIdx >= 0) a.setArticle(c.getString(artIdx));
        int uaIdx = c.getColumnIndex(DatabaseHelper.ART_UA);
        if (uaIdx >= 0) a.setUa(c.getString(uaIdx));
        int uvIdx = c.getColumnIndex(DatabaseHelper.ART_UV);
        if (uvIdx >= 0) a.setUv(c.getString(uvIdx));
        int coefIdx = c.getColumnIndex(DatabaseHelper.ART_COEF);
        if (coefIdx >= 0) a.setCoef(c.getDouble(coefIdx));
        int paIdx = c.getColumnIndex(DatabaseHelper.ART_PA);
        if (paIdx >= 0) a.setPa(c.getDouble(paIdx));
        int pvIdx = c.getColumnIndex(DatabaseHelper.ART_PV);
        if (pvIdx >= 0) a.setPv(c.getDouble(pvIdx));
        int qsIdx = c.getColumnIndex(DatabaseHelper.ART_QS);
        if (qsIdx >= 0) a.setQs(c.getDouble(qsIdx));
        int obsIdx = c.getColumnIndex(DatabaseHelper.ART_OBS);
        if (obsIdx >= 0) a.setObs(c.getString(obsIdx));
        // Jointure catégorie
        int catIdx = c.getColumnIndex("c_cat");
        if (catIdx >= 0) a.setNomCategorie(c.getString(catIdx));
        return a;
    }

    private ContentValues toContentValues(Article a) {
        ContentValues cv = new ContentValues();
        if (a.getCodeBarre() != null && !a.getCodeBarre().isEmpty())
            cv.put(DatabaseHelper.ART_CODEB, a.getCodeBarre());
        else
            cv.putNull(DatabaseHelper.ART_CODEB);
        cv.put(DatabaseHelper.ART_ART,     a.getArt());
        cv.put(DatabaseHelper.ART_IDC,     a.getIdCategorie());
        cv.put(DatabaseHelper.ART_ARTICLE, a.getArticle());
        cv.put(DatabaseHelper.ART_UA,      a.getUa());
        cv.put(DatabaseHelper.ART_UV,      a.getUv());
        cv.put(DatabaseHelper.ART_COEF,    a.getCoef());
        cv.put(DatabaseHelper.ART_PA,      a.getPa());
        cv.put(DatabaseHelper.ART_PV,      a.getPv());
        cv.put(DatabaseHelper.ART_QS,      a.getQs());
        cv.put(DatabaseHelper.ART_OBS,     a.getObs());
        return cv;
    }

    // ── CRUD ─────────────────────────────────────────────────
    public long inserer(Article a) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DatabaseHelper.T_ARTICLES, null, toContentValues(a));
    }

    public int modifier(Article a) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(DatabaseHelper.T_ARTICLES, toContentValues(a),
                DatabaseHelper.ART_ID + "=?",
                new String[]{String.valueOf(a.getId())});
    }

    public int supprimer(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try (Cursor c = db.rawQuery("SELECT 1 FROM journee_detail WHERE id_a=?", new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) return -2; // Utilisé
        }
        return db.delete(DatabaseHelper.T_ARTICLES,
                DatabaseHelper.ART_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    /** Met à jour uniquement le stock */
    public void mettreAJourStock(SQLiteDatabase db, long idArticle, double nouveauStock) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.ART_QS, nouveauStock);
        db.update(DatabaseHelper.T_ARTICLES, cv,
                DatabaseHelper.ART_ID + "=?",
                new String[]{String.valueOf(idArticle)});
    }

    // ── Requêtes ─────────────────────────────────────────────
    private static final String SQL_SELECT_ALL =
        "SELECT a.*, c.c_cat FROM articles a " +
        "LEFT JOIN categorie c ON a.id_c = c.c_id ";

    public List<Article> listerTous() {
        List<Article> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SQL_SELECT_ALL + "ORDER BY a.a_art", null)) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    public List<Article> rechercher(String terme) {
        List<Article> list = new ArrayList<>();
        if (terme == null || terme.trim().isEmpty()) return listerTous();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String like = "%" + terme.trim() + "%";
        try (Cursor c = db.rawQuery(
                SQL_SELECT_ALL + "WHERE a.a_art LIKE ? OR a.a_codeb LIKE ? OR a.a_article LIKE ? " +
                "ORDER BY a.a_art",
                new String[]{like, like, like})) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    public Article parId(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SQL_SELECT_ALL + "WHERE a.a_id=?",
                new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) return fromCursor(c);
        }
        return null;
    }

    /** Cherche par code barre (scan) */
    public Article parCodeBarre(String code) {
        if (code == null || code.isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SQL_SELECT_ALL + "WHERE a.a_codeb=?",
                new String[]{code})) {
            if (c.moveToFirst()) return fromCursor(c);
        }
        return null;
    }

    /** Cherche par libellé exact (insensible à la casse) */
    public Article parArt(String art) {
        if (art == null || art.isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SQL_SELECT_ALL + "WHERE LOWER(a.a_art)=LOWER(?)",
                new String[]{art.trim()})) {
            if (c.moveToFirst()) return fromCursor(c);
        }
        return null;
    }

    /** Vérifie si a_art existe déjà (pour validation saisie) */
    public boolean artExiste(String art, long excluId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM articles WHERE LOWER(a_art)=LOWER(?) AND a_id!=?",
                new String[]{art.trim(), String.valueOf(excluId)})) {
            return c.moveToFirst();
        }
    }

    /** Vérifie si code barre existe déjà */
    public boolean codeBExiste(String code, long excluId) {
        if (code == null || code.isEmpty()) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM articles WHERE a_codeb=? AND a_id!=?",
                new String[]{code, String.valueOf(excluId)})) {
            return c.moveToFirst();
        }
    }

    /** Articles en rupture de stock */
    public List<Article> enRupture() {
        List<Article> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(SQL_SELECT_ALL + "WHERE a.qs <= 0 ORDER BY a.a_art", null)) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }
}

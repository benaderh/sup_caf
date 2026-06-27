package com.supcaf.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.models.Tiers;
import java.util.ArrayList;
import java.util.List;

public class TiersDao {
    private final DatabaseHelper dbHelper;

    public TiersDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    private Tiers fromCursor(Cursor c) {
        Tiers t = new Tiers();
        t.setId(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.TRS_ID)));
        t.setTier(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.TRS_TIER)));
        t.setType(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.TRS_TYPE)));
        int obsIdx = c.getColumnIndex(DatabaseHelper.TRS_OBS);
        if (obsIdx >= 0) t.setObs(c.getString(obsIdx));
        int soldeIdx = c.getColumnIndex("solde");
        if (soldeIdx >= 0) t.setSolde(c.getDouble(soldeIdx));
        return t;
    }

    private ContentValues toContentValues(Tiers t) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.TRS_TIER, t.getTier());
        cv.put(DatabaseHelper.TRS_TYPE, t.getType());
        cv.put(DatabaseHelper.TRS_OBS,  t.getObs());
        return cv;
    }

    public long inserer(Tiers t) {
        return dbHelper.getWritableDatabase()
                .insert(DatabaseHelper.T_TIERS, null, toContentValues(t));
    }

    public int modifier(Tiers t) {
        return dbHelper.getWritableDatabase()
                .update(DatabaseHelper.T_TIERS, toContentValues(t),
                        DatabaseHelper.TRS_ID + "=?",
                        new String[]{String.valueOf(t.getId())});
    }

    public int supprimer(long id) {
        return dbHelper.getWritableDatabase()
                .delete(DatabaseHelper.T_TIERS,
                        DatabaseHelper.TRS_ID + "=?",
                        new String[]{String.valueOf(id)});
    }

    public Tiers parId(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT * FROM tiers WHERE t_id=?", new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) return fromCursor(c);
        }
        return null;
    }

    // ── Listes avec solde calculé ────────────────────────────

    /**
     * Fournisseurs avec solde dette :
     * dette = SUM(achats_net) - SUM(j_dec sur achats) - SUM(j_dec sur VF)
     */
    public List<Tiers> listerFournisseurs() {
        return listerAvecSolde("F",
            // Dettes = net achat - décaissé achats - versements directs
            "COALESCE((SELECT SUM(j_mt-j_remise-j_dec) FROM journee " +
            "  WHERE id_t=t.t_id AND j_type='A'),0) - " +
            "COALESCE((SELECT SUM(j_dec) FROM journee " +
            "  WHERE id_t=t.t_id AND j_type='VF'),0)");
    }

    /**
     * Clients avec solde créance :
     * créance = SUM(ventes_net) - SUM(j_enc sur ventes) - SUM(j_enc sur EC)
     */
    public List<Tiers> listerClients() {
        return listerAvecSolde("C",
            "COALESCE((SELECT SUM(j_mt-j_remise-j_enc) FROM journee " +
            "  WHERE id_t=t.t_id AND j_type='V'),0) - " +
            "COALESCE((SELECT SUM(j_enc) FROM journee " +
            "  WHERE id_t=t.t_id AND j_type='EC'),0)");
    }

    public List<Tiers> listerAutres() {
        return listerAvecSolde("A", "0");
    }

    private List<Tiers> listerAvecSolde(String type, String soldeExpr) {
        List<Tiers> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT t.*, (" + soldeExpr + ") AS solde " +
                     "FROM tiers t WHERE t.t_type=? ORDER BY t.t_tier";
        try (Cursor c = db.rawQuery(sql, new String[]{type})) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    /** Tous les tiers d'un type pour Spinner */
    public List<Tiers> listerParType(String type) {
        List<Tiers> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT * FROM tiers WHERE t_type=? ORDER BY t_tier",
                new String[]{type})) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    /** Solde d'un tiers spécifique */
    public double getSolde(long tiersId, String typeTiers) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql;
        if ("F".equals(typeTiers)) {
            sql = "SELECT COALESCE(SUM(j_mt-j_remise-j_dec),0) FROM journee " +
                  "WHERE id_t=? AND j_type='A'";
        } else if ("C".equals(typeTiers)) {
            sql = "SELECT COALESCE(SUM(j_mt-j_remise-j_enc),0) FROM journee " +
                  "WHERE id_t=? AND j_type='V'";
        } else {
            return 0;
        }
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(tiersId)})) {
            if (c.moveToFirst()) return c.getDouble(0);
        }
        return 0;
    }

    /** Total dettes tous fournisseurs confondus */
    public double totalDettes() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(j_mt-j_remise-j_dec),0) FROM journee " +
                "WHERE j_type='A' AND (j_mt-j_remise-j_dec)>0", null)) {
            if (c.moveToFirst()) return c.getDouble(0);
        }
        return 0;
    }

    /** Total créances tous clients confondus */
    public double totalCreances() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(j_mt-j_remise-j_enc),0) FROM journee " +
                "WHERE j_type='V' AND (j_mt-j_remise-j_enc)>0", null)) {
            if (c.moveToFirst()) return c.getDouble(0);
        }
        return 0;
    }
}

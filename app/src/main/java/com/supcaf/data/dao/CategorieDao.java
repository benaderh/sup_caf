package com.supcaf.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.models.Categorie;
import java.util.ArrayList;
import java.util.List;

public class CategorieDao {
    private final DatabaseHelper dbHelper;

    public CategorieDao(DatabaseHelper dbHelper) { this.dbHelper = dbHelper; }

    public List<Categorie> listerToutes() {
        List<Categorie> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM categorie ORDER BY c_cat", null)) {
            while (c.moveToNext()) {
                Categorie cat = new Categorie();
                cat.setId(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.CAT_ID)));
                cat.setCat(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.CAT_CAT)));
                int obsIdx = c.getColumnIndex(DatabaseHelper.CAT_OBS);
                if (obsIdx >= 0) cat.setObs(c.getString(obsIdx));
                list.add(cat);
            }
        }
        return list;
    }

    public long inserer(String nom) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.CAT_CAT, nom);
        return dbHelper.getWritableDatabase().insert(DatabaseHelper.T_CATEGORIE, null, cv);
    }

    public int supprimer(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try (Cursor c = db.rawQuery("SELECT 1 FROM articles WHERE id_c=?", new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) return -2; // Utilisé
        }
        return db.delete(DatabaseHelper.T_CATEGORIE, DatabaseHelper.CAT_ID + "=?", new String[]{String.valueOf(id)});
    }
}

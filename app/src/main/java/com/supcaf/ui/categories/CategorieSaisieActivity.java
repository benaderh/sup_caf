package com.supcaf.ui.categories;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.supcaf.R;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.dao.CategorieDao;
import com.supcaf.data.models.Categorie;

public class CategorieSaisieActivity extends AppCompatActivity {

    private CategorieDao categorieDao;
    private Categorie categorieEnCours;

    private TextInputEditText etCat, etObs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categorie_saisie);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        categorieDao = new CategorieDao(DatabaseHelper.getInstance(this));

        etCat = findViewById(R.id.et_cat);
        etObs = findViewById(R.id.et_obs);

        long id = getIntent().getLongExtra("categorie_id", -1);
        if (id != -1) {
            categorieEnCours = categorieDao.parId(id);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Modifier catégorie");
            remplirFormulaire();
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Nouvelle catégorie");
            categorieEnCours = new Categorie();
        }

        Button btnSuppr = findViewById(R.id.btn_supprimer);
        if (btnSuppr != null) {
            if (id != -1) {
                btnSuppr.setVisibility(View.VISIBLE);
                btnSuppr.setOnClickListener(v -> confirmerSuppression());
            } else {
                btnSuppr.setVisibility(View.GONE);
            }
        }

        findViewById(R.id.btn_enregistrer).setOnClickListener(v -> enregistrer());
        findViewById(R.id.btn_annuler).setOnClickListener(v -> finish());
    }

    private void remplirFormulaire() {
        if (categorieEnCours == null) return;
        etCat.setText(categorieEnCours.getCat());
        etObs.setText(categorieEnCours.getObs() != null ? categorieEnCours.getObs() : "");
    }

    private void enregistrer() {
        String cat = etCat.getText() != null ? etCat.getText().toString().trim() : "";
        if (cat.isEmpty()) {
            etCat.setError(getString(R.string.champ_obligatoire));
            etCat.requestFocus();
            return;
        }

        if (categorieEnCours == null) categorieEnCours = new Categorie();
        categorieEnCours.setCat(cat);
        categorieEnCours.setObs(etObs.getText() != null ? etObs.getText().toString() : "");

        if (categorieEnCours.getId() == 0) {
            long newId = categorieDao.inserer(categorieEnCours);
            if (newId > 0) {
                Toast.makeText(this, getString(R.string.enregistre), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, getString(R.string.erreur_saisie), Toast.LENGTH_SHORT).show();
            }
        } else {
            categorieDao.modifier(categorieEnCours);
            Toast.makeText(this, getString(R.string.enregistre), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void confirmerSuppression() {
        if (categorieEnCours == null || categorieEnCours.getId() == 0) return;
        new AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirm_supprimer))
            .setPositiveButton(R.string.oui, (d, w) -> {
                int res = categorieDao.supprimer(categorieEnCours.getId());
                if (res == -2) {
                    Toast.makeText(this, "Impossible : Catégorie utilisée dans des articles", Toast.LENGTH_LONG).show();
                } else if (res > 0) {
                    Toast.makeText(this, "Supprimé", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .setNegativeButton(R.string.non, null).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

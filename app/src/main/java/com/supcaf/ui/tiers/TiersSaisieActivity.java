package com.supcaf.ui.tiers;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.supcaf.R;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.dao.TiersDao;
import com.supcaf.data.models.Tiers;

public class TiersSaisieActivity extends AppCompatActivity {

    private TiersDao dao;
    private Tiers tiersEnCours;
    private TextInputEditText etNom, etObs;
    private RadioGroup rgType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiers_saisie);
        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        dao     = new TiersDao(DatabaseHelper.getInstance(this));
        etNom   = findViewById(R.id.et_nom);
        etObs   = findViewById(R.id.et_obs);
        rgType  = findViewById(R.id.rg_type);
        long id = getIntent().getLongExtra("tiers_id", -1);
        String typeDefaut = getIntent().getStringExtra("type_defaut");
        if (id != -1) {
            tiersEnCours = dao.parId(id);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Modifier tiers");
            if (tiersEnCours != null) {
                etNom.setText(tiersEnCours.getTier());
                etObs.setText(tiersEnCours.getObs() != null ? tiersEnCours.getObs() : "");
                switch (tiersEnCours.getType()) {
                    case "F": rgType.check(R.id.rb_fourn); break;
                    case "C": rgType.check(R.id.rb_client); break;
                    default:  rgType.check(R.id.rb_autres);
                }
            }
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Nouveau tiers");
            tiersEnCours = new Tiers();
            if ("C".equals(typeDefaut)) rgType.check(R.id.rb_client);
            else if ("A".equals(typeDefaut)) rgType.check(R.id.rb_autres);
            else rgType.check(R.id.rb_fourn);
        }
        findViewById(R.id.btn_enregistrer).setOnClickListener(v -> enregistrer());
        findViewById(R.id.btn_annuler).setOnClickListener(v -> finish());
    }

    private void enregistrer() {
        String nom = etNom.getText() != null ? etNom.getText().toString().trim() : "";
        if (nom.isEmpty()) { etNom.setError(getString(R.string.champ_obligatoire)); etNom.requestFocus(); return; }
        String type = "F";
        int chk = rgType.getCheckedRadioButtonId();
        if (chk == R.id.rb_client) type = "C";
        else if (chk == R.id.rb_autres) type = "A";
        tiersEnCours.setTier(nom);
        tiersEnCours.setType(type);
        tiersEnCours.setObs(etObs.getText() != null ? etObs.getText().toString() : "");
        if (tiersEnCours.getId() == 0) {
            long newId = dao.inserer(tiersEnCours);
            if (newId > 0) { Toast.makeText(this, getString(R.string.enregistre), Toast.LENGTH_SHORT).show(); finish(); }
            else Toast.makeText(this, getString(R.string.erreur_saisie), Toast.LENGTH_SHORT).show();
        } else {
            dao.modifier(tiersEnCours);
            Toast.makeText(this, getString(R.string.enregistre), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

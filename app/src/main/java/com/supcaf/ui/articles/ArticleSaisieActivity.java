package com.supcaf.ui.articles;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.supcaf.R;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.dao.ArticleDao;
import com.supcaf.data.dao.CategorieDao;
import com.supcaf.data.models.Article;
import com.supcaf.data.models.Categorie;
import com.supcaf.utils.FormatUtils;
import java.util.List;

public class ArticleSaisieActivity extends AppCompatActivity {

    private ArticleDao articleDao;
    private CategorieDao categorieDao;
    private Article articleEnCours;
    private List<Categorie> categories;

    private TextInputEditText etCodeBarre, etArt, etArticle, etUa, etUv, etCoef, etPa, etPv, etQs, etObs;
    private Spinner spinCategorie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_saisie);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        articleDao   = new ArticleDao(DatabaseHelper.getInstance(this));
        categorieDao = new CategorieDao(DatabaseHelper.getInstance(this));
        lierVues();
        chargerCategories();
        long id = getIntent().getLongExtra("article_id", -1);
        if (id != -1) {
            articleEnCours = articleDao.parId(id);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Modifier article");
            remplirFormulaire();
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Nouvel article");
            articleEnCours = new Article();
            String codePre = getIntent().getStringExtra("code_barre");
            if (codePre != null) etCodeBarre.setText(codePre);
            String artPre = getIntent().getStringExtra("art");
            if (artPre != null) etArt.setText(artPre);
        }
        findViewById(R.id.btn_enregistrer).setOnClickListener(v -> enregistrer());
        findViewById(R.id.btn_annuler).setOnClickListener(v -> finish());
    }

    private void lierVues() {
        etCodeBarre  = findViewById(R.id.et_code_barre);
        etArt        = findViewById(R.id.et_art);
        etArticle    = findViewById(R.id.et_article);
        etUa         = findViewById(R.id.et_ua);
        etUv         = findViewById(R.id.et_uv);
        etCoef       = findViewById(R.id.et_coef);
        etPa         = findViewById(R.id.et_pa);
        etPv         = findViewById(R.id.et_pv);
        etQs         = findViewById(R.id.et_qs);
        etObs        = findViewById(R.id.et_obs);
        spinCategorie = findViewById(R.id.spin_categorie);
    }

    private void chargerCategories() {
        categories = categorieDao.listerToutes();
        String[] noms = new String[categories.size() + 1];
        noms[0] = "— Sans catégorie —";
        for (int i = 0; i < categories.size(); i++) noms[i + 1] = categories.get(i).getCat();
        spinCategorie.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, noms));
    }

    private void remplirFormulaire() {
        if (articleEnCours == null) return;
        etCodeBarre.setText(articleEnCours.getCodeBarre() != null ? articleEnCours.getCodeBarre() : "");
        etArt.setText(articleEnCours.getArt());
        etArticle.setText(articleEnCours.getArticle() != null ? articleEnCours.getArticle() : "");
        etUa.setText(articleEnCours.getUa());
        etUv.setText(articleEnCours.getUv());
        etCoef.setText(String.valueOf(articleEnCours.getCoef()));
        etPa.setText(String.valueOf(articleEnCours.getPa()));
        etPv.setText(String.valueOf(articleEnCours.getPv()));
        etQs.setText(String.valueOf(articleEnCours.getQs()));
        etObs.setText(articleEnCours.getObs() != null ? articleEnCours.getObs() : "");
        long idCat = articleEnCours.getIdCategorie();
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == idCat) { spinCategorie.setSelection(i + 1); break; }
        }
    }

    private void enregistrer() {
        String art = etArt.getText() != null ? etArt.getText().toString().trim() : "";
        if (art.isEmpty()) { etArt.setError(getString(R.string.champ_obligatoire)); etArt.requestFocus(); return; }
        long excluId = articleEnCours != null ? articleEnCours.getId() : 0;
        if (articleDao.artExiste(art, excluId)) { etArt.setError(getString(R.string.article_existe_deja)); etArt.requestFocus(); return; }
        String cb = etCodeBarre.getText() != null ? etCodeBarre.getText().toString().trim() : "";
        if (!cb.isEmpty() && articleDao.codeBExiste(cb, excluId)) { etCodeBarre.setError("Code barre déjà utilisé"); etCodeBarre.requestFocus(); return; }
        if (articleEnCours == null) articleEnCours = new Article();
        articleEnCours.setCodeBarre(cb.isEmpty() ? null : cb);
        articleEnCours.setArt(art);
        articleEnCours.setArticle(etArticle.getText() != null ? etArticle.getText().toString() : "");
        String ua = etUa.getText() != null ? etUa.getText().toString().trim() : "";
        articleEnCours.setUa(ua.isEmpty() ? "U" : ua);
        String uv = etUv.getText() != null ? etUv.getText().toString().trim() : "";
        articleEnCours.setUv(uv.isEmpty() ? "U" : uv);
        articleEnCours.setCoef(FormatUtils.parseDouble(etCoef.getText() != null ? etCoef.getText().toString() : "1"));
        articleEnCours.setPa(FormatUtils.parseDouble(etPa.getText() != null ? etPa.getText().toString() : "0"));
        articleEnCours.setPv(FormatUtils.parseDouble(etPv.getText() != null ? etPv.getText().toString() : "0"));
        articleEnCours.setQs(FormatUtils.parseDouble(etQs.getText() != null ? etQs.getText().toString() : "0"));
        articleEnCours.setObs(etObs.getText() != null ? etObs.getText().toString() : "");
        int spinPos = spinCategorie.getSelectedItemPosition();
        articleEnCours.setIdCategorie(spinPos > 0 && spinPos - 1 < categories.size() ? categories.get(spinPos - 1).getId() : 0);
        if (articleEnCours.getId() == 0) {
            long newId = articleDao.inserer(articleEnCours);
            if (newId > 0) { Toast.makeText(this, getString(R.string.enregistre), Toast.LENGTH_SHORT).show(); finish(); }
            else Toast.makeText(this, getString(R.string.erreur_saisie), Toast.LENGTH_SHORT).show();
        } else {
            articleDao.modifier(articleEnCours);
            Toast.makeText(this, getString(R.string.enregistre), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}

package com.supcaf.ui.achats;

import android.content.Intent;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.supcaf.R;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.dao.*;
import com.supcaf.data.models.*;
import com.supcaf.ui.articles.ArticleSaisieActivity;
import com.supcaf.utils.FormatUtils;
import java.util.ArrayList;
import java.util.List;

public class AchatSaisieActivity extends AppCompatActivity {

    private JourneeDao journeeDao;
    private ArticleDao articleDao;
    private TiersDao   tiersDao;
    private CategorieDao categorieDao;

    private TextInputEditText etDate, etHeure, etCodeBarre, etQte, etPrix, etRemise, etDec, etReste;
    private AutoCompleteTextView etArt;
    private Spinner spinFournisseur, spinCategorieDetail;
    private TextView tvTotal, tvNet;
    private RecyclerView rvLignes;

    private List<JourneeDetail> lignes = new ArrayList<>();
    private LignesAdapter lignesAdapter;
    private List<Tiers> fournisseurs;
    private List<Categorie> categories;
    private Journee journeeExistante = null;
    private int currentEditingLineIndex = -1;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if(result.getContents() != null) {
            etCodeBarre.setText(result.getContents());
            selectionnerArticleParCodeBarre();
        } else {
            Toast.makeText(this, "Scan annulé", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achat_saisie);
        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Saisie achat");
        }

        journeeDao = new JourneeDao(DatabaseHelper.getInstance(this));
        articleDao = new ArticleDao(DatabaseHelper.getInstance(this));
        tiersDao   = new TiersDao(DatabaseHelper.getInstance(this));
        categorieDao = new CategorieDao(DatabaseHelper.getInstance(this));

        lierVues();
        chargerFournisseurs();
        chargerCategoriesDetail();

        TextInputLayout tilCodeBarre = findViewById(R.id.til_code_barre);
        if (tilCodeBarre != null) {
            tilCodeBarre.setEndIconOnClickListener(v -> lancerScannerCodeBarre());
        }

        long jId = getIntent().getLongExtra("journee_id", -1);
        if (jId != -1) {
            journeeExistante = journeeDao.parId(jId);
            if (journeeExistante != null) {
                lignes.addAll(journeeDao.listerDetails(jId));
                remplirEntete();
            }
        } else {
            String[] dh = FormatUtils.dateHeureAujourdhuiArray();
            etDate.setText(dh[0]);
            etHeure.setText(dh[1]);
        }

        lignesAdapter = new LignesAdapter(lignes, this::supprimerLigne, this::modifierLigne);
        rvLignes.setLayoutManager(new LinearLayoutManager(this));
        rvLignes.setAdapter(lignesAdapter);
        
        chargerArticlesAutocomplete();
        recalculer();

        etCodeBarre.setOnEditorActionListener((tv, actionId, event) -> { selectionnerArticleParCodeBarre(); return true; });

        etRemise.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { recalculer(); }
            public void afterTextChanged(android.text.Editable s) {}
        });
        etDec.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { recalculer(); }
            public void afterTextChanged(android.text.Editable s) {}
        });
        etReste.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { recalculer(); }
            public void afterTextChanged(android.text.Editable s) {}
        });

        findViewById(R.id.btn_enregistrer).setOnClickListener(v -> enregistrer());
        findViewById(R.id.btn_annuler).setOnClickListener(v -> finish());
        
        Button btnSuppr = findViewById(R.id.btn_supprimer_journee);
        if (btnSuppr != null) {
            if (journeeExistante != null) {
                btnSuppr.setVisibility(View.VISIBLE);
                btnSuppr.setOnClickListener(v -> confirmerSuppression());
            } else {
                btnSuppr.setVisibility(View.GONE);
            }
        }
    }

    private void confirmerSuppression() {
        if (journeeExistante == null) return;
        new AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirm_supprimer))
            .setPositiveButton(R.string.oui, (d, w) -> {
                boolean ok = journeeDao.supprimer(journeeExistante.getId());
                if (ok) {
                    Toast.makeText(this, "Achat supprimé", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.non, null).show();
    }

    private void lierVues() {
        etDate        = findViewById(R.id.et_date);
        etHeure       = findViewById(R.id.et_heure);
        etCodeBarre   = findViewById(R.id.et_code_barre);
        etArt         = findViewById(R.id.et_art);
        etQte         = findViewById(R.id.et_qte);
        etPrix        = findViewById(R.id.et_prix);
        etRemise      = findViewById(R.id.et_remise);
        etDec         = findViewById(R.id.et_dec);
        etReste       = findViewById(R.id.et_reste);
        spinFournisseur = findViewById(R.id.spin_fournisseur);
        spinCategorieDetail = findViewById(R.id.spin_categorie_detail);
        tvTotal       = findViewById(R.id.tv_total);
        tvNet         = findViewById(R.id.tv_net);
        rvLignes      = findViewById(R.id.rv_lignes);

        installerSelectionFocus(etRemise, etDec, etReste);

        etQte.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) etQte.selectAll();
            else updateCurrentLine();
        });
        etPrix.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) etPrix.selectAll();
            else updateCurrentLine();
        });
    }
    
    private void showDatePicker() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            etDate.setText(String.format("%02d/%02d/%04d", d, m+1, y));
        }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        new android.app.TimePickerDialog(this, (view, h, m) -> {
            etHeure.setText(String.format("%02d:%02d", h, m));
        }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), true).show();
    }

    private void chargerArticlesAutocomplete() {
        List<Article> articles = articleDao.listerParCategorie(idCategorieSelectionnee());
        ArrayAdapter<Article> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, articles);
        etArt.setAdapter(adapter);
        etArt.setOnItemClickListener((parent, view, position, id) -> {
            Article a = (Article) parent.getItemAtPosition(position);
            ajouterArticleALaListe(a);
        });
    }

    private void chargerCategoriesDetail() {
        categories = categorieDao.listerToutes();
        String[] noms = new String[categories.size() + 1];
        noms[0] = "Toutes categories";
        for (int i = 0; i < categories.size(); i++) noms[i + 1] = categories.get(i).getCat();
        spinCategorieDetail.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, noms));
        spinCategorieDetail.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { chargerArticlesAutocomplete(); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private long idCategorieSelectionnee() {
        int pos = spinCategorieDetail.getSelectedItemPosition();
        return pos > 0 && categories != null && pos - 1 < categories.size()
                ? categories.get(pos - 1).getId()
                : 0;
    }
    
    private void lancerScannerCodeBarre() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("Scannez un code barre");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(true);
        options.setCaptureActivity(com.supcaf.utils.CaptureActivityPortrait.class);
        barcodeLauncher.launch(options);
    }

    private void chargerFournisseurs() {
        fournisseurs = tiersDao.listerParType("F");
        String[] noms = new String[fournisseurs.size()];
        int defaultPos = 0;
        for (int i = 0; i < fournisseurs.size(); i++) {
            noms[i] = fournisseurs.get(i).getTier();
            if (estComptant(fournisseurs.get(i), DatabaseHelper.FOURN_COMPTANT_ID)) defaultPos = i;
        }
        spinFournisseur.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, noms));
        if (!fournisseurs.isEmpty()) spinFournisseur.setSelection(defaultPos);
        spinFournisseur.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { recalculer(); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void remplirEntete() {
        if (journeeExistante == null) return;
        String dateComplete = journeeExistante.getDate();
        if (dateComplete != null) {
            if (dateComplete.contains(" ")) {
                String[] parts = dateComplete.split(" ");
                etDate.setText(FormatUtils.dateAffichage(parts[0]));
                etHeure.setText(parts[1]);
            } else {
                etDate.setText(FormatUtils.dateAffichage(dateComplete));
                etHeure.setText("");
            }
        }
        etRemise.setText(String.valueOf(journeeExistante.getRemise()));
        etDec.setText(String.valueOf(journeeExistante.getDec()));
        long idT = journeeExistante.getIdTiers();
        for (int i = 0; i < fournisseurs.size(); i++) {
            if (fournisseurs.get(i).getId() == idT) { spinFournisseur.setSelection(i); break; }
        }
    }

    private void ajouterArticleALaListe(Article article) {
        double qte = 1;
        double prix = article.getPa();
        if (prix <= 0) prix = 0;

        JourneeDetail ligne = new JourneeDetail(article.getId(), article.getArt(), qte, prix, article.getUa());
        lignes.add(ligne);
        currentEditingLineIndex = lignes.size() - 1;
        lignesAdapter.notifyItemInserted(currentEditingLineIndex);

        etCodeBarre.setText(article.getCodeBarre() != null ? article.getCodeBarre() : "");
        etArt.setText(article.getArt(), false);
        etQte.setText("1");
        etPrix.setText(FormatUtils.montantSansDevise(prix));
        
        etQte.requestFocus();
        recalculer();
    }

    private void supprimerLigne(int pos) {
        lignes.remove(pos);
        lignesAdapter.notifyItemRemoved(pos);
        if (currentEditingLineIndex == pos) {
            currentEditingLineIndex = -1;
            etCodeBarre.setText("");
            etArt.setText("", false);
            etQte.setText("1");
            etPrix.setText("");
        } else if (currentEditingLineIndex > pos) {
            currentEditingLineIndex--;
        }
        recalculer();
    }

    private void modifierLigne(int pos) {
        currentEditingLineIndex = pos;
        JourneeDetail d = lignes.get(pos);
        Article a = articleDao.parId(d.getIdArticle());
        if (a != null && a.getCodeBarre() != null) etCodeBarre.setText(a.getCodeBarre());
        else etCodeBarre.setText("");
        etArt.setText(d.getNomArticle(), false);
        etQte.setText(FormatUtils.montantSansDevise(d.getQuantite()));
        etPrix.setText(FormatUtils.montantSansDevise(d.getPrix()));
        etQte.requestFocus();
    }

    private void updateCurrentLine() {
        if (currentEditingLineIndex != -1 && currentEditingLineIndex < lignes.size()) {
            double qte  = FormatUtils.parseDouble(etQte.getText() != null ? etQte.getText().toString() : "1");
            double prix = FormatUtils.parseDouble(etPrix.getText() != null ? etPrix.getText().toString() : "0");
            JourneeDetail d = lignes.get(currentEditingLineIndex);
            if (d.getQuantite() != qte || d.getPrix() != prix) {
                d.setQuantite(qte);
                d.setPrix(prix);
                lignesAdapter.notifyItemChanged(currentEditingLineIndex);
                recalculer();
            }
        }
    }

    private boolean isRecalculating = false;

    private void selectionnerArticleParCodeBarre() {
        String code = etCodeBarre.getText() != null ? etCodeBarre.getText().toString().trim() : "";
        if (code.isEmpty()) return;
        Article article = articleDao.parCodeBarre(code);
        if (article == null) {
            Intent intent = new Intent(this, ArticleSaisieActivity.class);
            new AlertDialog.Builder(this)
                .setMessage(getString(R.string.article_non_trouve) + "\n\"" + code + "\"")
                .setPositiveButton("Creer", (d, w) -> {
                    intent.putExtra("code_barre", code);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.non, null).show();
            return;
        }
        ajouterArticleALaListe(article);
    }

    private void recalculer() {
        if (isRecalculating) return;
        isRecalculating = true;
        try {
            double total = 0;
            for (JourneeDetail d : lignes) total += d.getMontantTotal();
            
            boolean isComptant = false;
            int pos = spinFournisseur.getSelectedItemPosition();
            if (pos >= 0 && pos < fournisseurs.size()) {
                isComptant = estComptant(fournisseurs.get(pos), DatabaseHelper.FOURN_COMPTANT_ID);
            }

            double remise = FormatUtils.parseDouble(etRemise.getText() != null ? etRemise.getText().toString() : "0");
            double dec    = FormatUtils.parseDouble(etDec.getText() != null ? etDec.getText().toString() : "0");
            
            if (isComptant) {
                // Reste = 0 toujours. Remise = Total - Dec (si Dec > 0)
                findViewById(R.id.layout_reste).setVisibility(View.GONE);
                etReste.setText("0");
                remise = Math.max(0, total - dec);
                etRemise.setText(FormatUtils.montantSansDevise(remise));
            } else {
                findViewById(R.id.layout_reste).setVisibility(View.VISIBLE);
                double reste = FormatUtils.parseDouble(etReste.getText() != null ? etReste.getText().toString() : "0");
                // Si le Reste a été modifié manuellement, on pourrait recalculer Dec.
                // Pour faire simple, on affiche juste le résultat du calcul.
                if (etReste.hasFocus()) {
                    remise = Math.max(0, total - dec - reste);
                    etRemise.setText(FormatUtils.montantSansDevise(remise));
                } else {
                    reste = Math.max(0, total - remise - dec);
                    etReste.setText(FormatUtils.montantSansDevise(reste));
                }
            }

            tvTotal.setText("Total: " + FormatUtils.montant(total));
            tvNet.setText("Net: " + FormatUtils.montant(total - remise));
            tvNet.setVisibility(View.VISIBLE);
        } finally {
            isRecalculating = false;
        }
    }

    private void enregistrer() {
        updateCurrentLine(); // flush pending edits
        if (lignes.isEmpty()) {
            Toast.makeText(this, getString(R.string.aucun_article), Toast.LENGTH_SHORT).show();
            return;
        }
        Journee j = journeeExistante != null ? journeeExistante : new Journee();
        String dateVal = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String heureVal = etHeure.getText() != null ? etHeure.getText().toString().trim() : "";
        dateVal = FormatUtils.dateBd(dateVal); // convertit en yyyy-MM-dd
        if (!heureVal.isEmpty()) dateVal += " " + heureVal;
        j.setDate(dateVal.isEmpty() ? FormatUtils.dateAujourdhui() : dateVal);
        j.setType(DatabaseHelper.TYPE_ACHAT);
        j.setLibelle("");

        j.setDetails(lignes);
        j.recalculerMontant();
        double dec    = FormatUtils.parseDouble(etDec.getText() != null ? etDec.getText().toString() : "0");
        double reste  = FormatUtils.parseDouble(etReste.getText() != null ? etReste.getText().toString() : "0");
        long idTiers = idTiersSelectionne();
        double remise = idTiers == DatabaseHelper.FOURN_COMPTANT_ID
                ? Math.max(0, j.getMt() - dec)
                : Math.max(0, j.getMt() - dec - reste);
        j.setRemise(remise);
        j.setDec(dec);
        j.setEnc(0);
        j.setIdTiers(idTiers);

        boolean ok = (journeeExistante != null) ? journeeDao.modifierOperation(j, true) : (journeeDao.enregistrerOperation(j, true) > 0);
        if (ok) {
            Toast.makeText(this, getString(R.string.enregistre), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, getString(R.string.erreur_saisie), Toast.LENGTH_SHORT).show();
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private long idTiersSelectionne() {
        int pos = spinFournisseur.getSelectedItemPosition();
        return (pos >= 0 && pos < fournisseurs.size())
                ? fournisseurs.get(pos).getId()
                : DatabaseHelper.FOURN_COMPTANT_ID;
    }

    private boolean estComptant(Tiers tiers, long comptantId) {
        return tiers != null && (tiers.getId() == comptantId ||
                (tiers.getTier() != null && tiers.getTier().toLowerCase().contains("comptant")));
    }

    private void installerSelectionFocus(TextInputEditText... champs) {
        for (TextInputEditText champ : champs) {
            champ.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) champ.selectAll();
            });
        }
    }

    // ── Adapter lignes ───────────────────────────────────────
    static class LignesAdapter extends RecyclerView.Adapter<LignesAdapter.VH> {
        interface OnAction { void on(int pos); }
        private final List<JourneeDetail> data;
        private final OnAction onSuppr;
        private final OnAction onEdit;
        LignesAdapter(List<JourneeDetail> d, OnAction s, OnAction e) { data = d; onSuppr = s; onEdit = e; }

        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            return new VH(android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_ligne_detail, p, false));
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            JourneeDetail d = data.get(pos);
            h.tvArt.setText(d.getNomArticle());
            h.tvQte.setText(FormatUtils.quantite(d.getQuantite()) + " × " + FormatUtils.montant(d.getPrix()));
            h.tvTotal.setText(FormatUtils.montant(d.getMontantTotal()));
            h.btnSuppr.setOnClickListener(v -> onSuppr.on(h.getAdapterPosition()));
            h.itemView.setOnClickListener(v -> {
                if (onEdit != null) onEdit.on(h.getAdapterPosition());
            });
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvArt, tvQte, tvTotal;
            ImageButton btnSuppr;
            VH(View v) {
                super(v);
                tvArt   = v.findViewById(R.id.tv_art);
                tvQte   = v.findViewById(R.id.tv_qte);
                tvTotal = v.findViewById(R.id.tv_total);
                btnSuppr = v.findViewById(R.id.btn_suppr);
            }
        }
    }
}

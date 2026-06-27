package com.supcaf.ui.ventes;

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

public class VenteSaisieActivity extends AppCompatActivity {

    private JourneeDao journeeDao;
    private ArticleDao articleDao;
    private TiersDao   tiersDao;

    private TextInputEditText etDate, etHeure, etCodeBarre, etQte, etPrix, etRemise, etEnc, etReste, etLibelle;
    private AutoCompleteTextView etArt;
    private Spinner spinClient, spinCategorieDetail;
    private TextView tvTotal, tvNet;
    private RecyclerView rvLignes;

    private List<JourneeDetail> lignes = new ArrayList<>();
    private LignesAdapter lignesAdapter;
    private List<Tiers> clients;
    private Journee journeeExistante = null;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if(result.getContents() != null) {
            etCodeBarre.setText(result.getContents());
            ajouterLigne();
        } else {
            Toast.makeText(this, "Scan annulé", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vente_saisie);
        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Saisie vente");
        }

        journeeDao = new JourneeDao(DatabaseHelper.getInstance(this));
        articleDao = new ArticleDao(DatabaseHelper.getInstance(this));
        tiersDao   = new TiersDao(DatabaseHelper.getInstance(this));

        lierVues();
        chargerClients();

        long jId = getIntent().getLongExtra("journee_id", -1);
        if (jId != -1) {
            journeeExistante = journeeDao.parId(jId);
            if (journeeExistante != null) {
                lignes.addAll(journeeDao.listerDetails(jId));
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
                etLibelle.setText(journeeExistante.getLibelle() != null ? journeeExistante.getLibelle() : "");
                etRemise.setText(String.valueOf(journeeExistante.getRemise()));
                etEnc.setText(String.valueOf(journeeExistante.getEnc()));
                long idT = journeeExistante.getIdTiers();
                for (int i = 0; i < clients.size(); i++)
                    if (clients.get(i).getId() == idT) { spinClient.setSelection(i); break; }
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

        findViewById(R.id.btn_ajouter_ligne).setOnClickListener(v -> ajouterLigne());
        etCodeBarre.setOnEditorActionListener((tv, id, ev) -> { ajouterLigne(); return true; });
        
        com.google.android.material.switchmaterial.SwitchMaterial switchScan = findViewById(R.id.switch_scan);
        if (switchScan != null) {
            switchScan.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    etCodeBarre.requestFocus();
                    Toast.makeText(this, "Mode Scan activé", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        com.google.android.material.textfield.TextInputLayout tilCodeBarre = findViewById(R.id.til_code_barre);
        if (tilCodeBarre != null) {
            tilCodeBarre.setEndIconOnClickListener(v -> lancerScannerCodeBarre());
        }

        etEnc.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { recalculer(); }
            public void afterTextChanged(android.text.Editable s) {}
        });
        etRemise.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { recalculer(); }
            public void afterTextChanged(android.text.Editable s) {}
        });

        findViewById(R.id.btn_enregistrer).setOnClickListener(v -> enregistrer());
        findViewById(R.id.btn_annuler).setOnClickListener(v -> finish());
        
        Button btnSuppr = findViewById(R.id.btn_supprimer_journee);
        if (btnSuppr != null) {
            if (jId != -1) {
                btnSuppr.setVisibility(View.VISIBLE);
                btnSuppr.setOnClickListener(v -> confirmerSuppression(jId));
            } else {
                btnSuppr.setVisibility(View.GONE);
            }
        }
    }

    private void confirmerSuppression(long id) {
        new AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirm_supprimer))
            .setPositiveButton(R.string.oui, (d, w) -> {
                boolean ok = journeeDao.supprimer(id);
                if (ok) {
                    Toast.makeText(this, "Vente supprimée", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.non, null).show();
    }

    private void lierVues() {
        etDate      = findViewById(R.id.et_date);
        etHeure     = findViewById(R.id.et_heure);
        etCodeBarre = findViewById(R.id.et_code_barre);
        etArt       = findViewById(R.id.et_art);
        etQte       = findViewById(R.id.et_qte);
        etPrix      = findViewById(R.id.et_prix);
        etRemise    = findViewById(R.id.et_remise);
        etEnc       = findViewById(R.id.et_enc);
        etReste     = findViewById(R.id.et_reste);
        etLibelle   = findViewById(R.id.et_libelle);
        spinClient  = findViewById(R.id.spin_client);
        spinCategorieDetail = findViewById(R.id.spin_categorie_detail);
        tvTotal     = findViewById(R.id.tv_total);
        tvNet       = findViewById(R.id.tv_net);
        rvLignes    = findViewById(R.id.rv_lignes);

        etDate.setOnClickListener(v -> showDatePicker());
        etHeure.setOnClickListener(v -> showTimePicker());
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
        List<Article> articles = articleDao.listerTous();
        ArrayAdapter<Article> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, articles);
        etArt.setAdapter(adapter);
        etArt.setOnItemClickListener((parent, view, position, id) -> {
            Article a = (Article) parent.getItemAtPosition(position);
            etCodeBarre.setText(a.getCodeBarre() != null ? a.getCodeBarre() : "");
            etArt.setText(a.getArt());
            etPrix.setText(String.valueOf(a.getPv()));
            etQte.requestFocus();
        });
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

    private void chargerClients() {
        clients = tiersDao.listerParType("C");
        String[] noms = new String[clients.size()];
        int defaultPos = 0;
        for (int i = 0; i < clients.size(); i++) {
            noms[i] = clients.get(i).getTier();
            if (noms[i].equalsIgnoreCase("Comptant")) defaultPos = i;
        }
        spinClient.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, noms));
        if (!clients.isEmpty()) spinClient.setSelection(defaultPos);
        spinClient.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { recalculer(); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void ajouterLigne() {
        String code = etCodeBarre.getText() != null ? etCodeBarre.getText().toString().trim() : "";
        String art  = etArt.getText()       != null ? etArt.getText().toString().trim()       : "";
        Article article = null;
        if (!code.isEmpty()) article = articleDao.parCodeBarre(code);
        if (article == null && !art.isEmpty()) article = articleDao.parArt(art);
        if (article == null) {
            String ref = !code.isEmpty() ? code : art;
            if (ref.isEmpty()) return;
            new AlertDialog.Builder(this)
                .setMessage(getString(R.string.article_non_trouve) + "\n« " + ref + " »")
                .setPositiveButton("Créer", (d, w) -> {
                    Intent intent = new Intent(this, ArticleSaisieActivity.class);
                    if (!code.isEmpty()) intent.putExtra("code_barre", code);
                    if (!art.isEmpty())  intent.putExtra("art", art);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.non, null).show();
            return;
        }
        double qte  = FormatUtils.parseDouble(etQte.getText() != null ? etQte.getText().toString() : "1");
        double prix = FormatUtils.parseDouble(etPrix.getText() != null ? etPrix.getText().toString() : "");
        if (qte <= 0) qte = 1;
        if (prix <= 0) prix = article.getPv(); // prix vente par défaut

        if (article.getQs() < qte) {
            Toast.makeText(this, getString(R.string.stock_insuffisant) +
                " (stock: " + FormatUtils.quantite(article.getQs()) + ")", Toast.LENGTH_SHORT).show();
        }

        lignes.add(new JourneeDetail(article.getId(), article.getArt(), qte, prix, article.getUv()));
        lignesAdapter.notifyItemInserted(lignes.size() - 1);
        etCodeBarre.setText(""); etArt.setText(""); etQte.setText("1"); etPrix.setText("");
        
        com.google.android.material.switchmaterial.SwitchMaterial switchScan = findViewById(R.id.switch_scan);
        if (switchScan != null && switchScan.isChecked()) {
            etCodeBarre.requestFocus();
        }
        
        recalculer();
    }

    private void supprimerLigne(int pos) {
        lignes.remove(pos);
        lignesAdapter.notifyItemRemoved(pos);
        recalculer();
    }

    private void modifierLigne(int pos) {
        JourneeDetail d = lignes.get(pos);
        etCodeBarre.setText(d.getNomArticle());
        etArt.setText(d.getNomArticle());
        etQte.setText(String.valueOf(d.getQuantite()));
        etPrix.setText(String.valueOf(d.getPrix()));
        supprimerLigne(pos);
        Toast.makeText(this, "Ligne prête à être modifiée en haut", Toast.LENGTH_SHORT).show();
    }

    private boolean isRecalculating = false;

    private void recalculer() {
        if (isRecalculating) return;
        isRecalculating = true;
        try {
            double total  = 0;
            for (JourneeDetail d : lignes) total += d.getMontantTotal();
            
            boolean isComptant = false;
            int pos = spinClient.getSelectedItemPosition();
            if (pos >= 0 && pos < clients.size()) {
                if (clients.get(pos).getTier().equalsIgnoreCase("Comptant")) isComptant = true;
            }

            double remise = FormatUtils.parseDouble(etRemise.getText() != null ? etRemise.getText().toString() : "0");
            double enc    = FormatUtils.parseDouble(etEnc.getText() != null ? etEnc.getText().toString() : "0");
            
            if (isComptant) {
                // Reste = 0 toujours. Remise = Total - Enc
                findViewById(R.id.layout_reste).setVisibility(View.GONE);
                etReste.setText("0");
            } else {
                findViewById(R.id.layout_reste).setVisibility(View.VISIBLE);
                double reste = total - remise - enc;
                etReste.setText(FormatUtils.montantSansDevise(reste));
            }

            tvTotal.setText("Total: " + FormatUtils.montant(total));
        } finally {
            isRecalculating = false;
        }
    }

    private void enregistrer() {
        if (lignes.isEmpty()) { Toast.makeText(this, getString(R.string.aucun_article), Toast.LENGTH_SHORT).show(); return; }
        Journee j = journeeExistante != null ? journeeExistante : new Journee();
        String dateVal = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String heureVal = etHeure.getText() != null ? etHeure.getText().toString().trim() : "";
        dateVal = FormatUtils.dateBd(dateVal); // convertit en yyyy-MM-dd
        if (!heureVal.isEmpty()) dateVal += " " + heureVal;
        j.setDate(dateVal.isEmpty() ? FormatUtils.dateAujourdhui() : dateVal);
        j.setType(DatabaseHelper.TYPE_VENTE);
        j.setLibelle(etLibelle.getText() != null ? etLibelle.getText().toString() : "");
        j.setDetails(lignes);
        j.recalculerMontant();
        double remise = FormatUtils.parseDouble(etRemise.getText() != null ? etRemise.getText().toString() : "0");
        double enc    = FormatUtils.parseDouble(etEnc.getText() != null ? etEnc.getText().toString() : "0");
        j.setRemise(remise);
        j.setEnc(enc);
        j.setDec(0);
        int pos = spinClient.getSelectedItemPosition();
        long idTiers = (pos >= 0 && pos < clients.size())
                ? clients.get(pos).getId()
                : DatabaseHelper.CLIENT_COMPTANT_ID;
        j.setIdTiers(idTiers);

        boolean ok = (journeeExistante != null) ? journeeDao.modifierOperation(j, false) : (journeeDao.enregistrerOperation(j, false) > 0);
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

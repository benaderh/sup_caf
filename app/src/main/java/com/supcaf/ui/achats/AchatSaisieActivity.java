package com.supcaf.ui.achats;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.*;
import com.google.android.material.textfield.TextInputEditText;
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

    private TextInputEditText etDate, etCodeBarre, etArt, etQte, etPrix, etRemise, etDec, etLibelle;
    private Spinner spinFournisseur;
    private TextView tvTotal, tvNet, tvReste;
    private RecyclerView rvLignes;

    private List<JourneeDetail> lignes = new ArrayList<>();
    private LignesAdapter lignesAdapter;
    private List<Tiers> fournisseurs;
    private Journee journeeExistante = null;

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

        lierVues();
        chargerFournisseurs();

        long jId = getIntent().getLongExtra("journee_id", -1);
        if (jId != -1) {
            journeeExistante = journeeDao.parId(jId);
            if (journeeExistante != null) {
                lignes.addAll(journeeDao.listerDetails(jId));
                remplirEntete();
            }
        } else {
            etDate.setText(FormatUtils.dateAujourdhui());
        }

        lignesAdapter = new LignesAdapter(lignes, this::supprimerLigne);
        rvLignes.setLayoutManager(new LinearLayoutManager(this));
        rvLignes.setAdapter(lignesAdapter);
        recalculer();

        // Recherche article par code barre ou libellé
        findViewById(R.id.btn_ajouter_ligne).setOnClickListener(v -> ajouterLigne());
        etCodeBarre.setOnEditorActionListener((tv, actionId, event) -> { ajouterLigne(); return true; });
        etDec.addTextChangedListener(new android.text.TextWatcher() {
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
    }

    private void lierVues() {
        etDate        = findViewById(R.id.et_date);
        etCodeBarre   = findViewById(R.id.et_code_barre);
        etArt         = findViewById(R.id.et_art);
        etQte         = findViewById(R.id.et_qte);
        etPrix        = findViewById(R.id.et_prix);
        etRemise      = findViewById(R.id.et_remise);
        etDec         = findViewById(R.id.et_dec);
        etLibelle     = findViewById(R.id.et_libelle);
        spinFournisseur = findViewById(R.id.spin_fournisseur);
        tvTotal       = findViewById(R.id.tv_total);
        tvNet         = findViewById(R.id.tv_net);
        tvReste       = findViewById(R.id.tv_reste);
        rvLignes      = findViewById(R.id.rv_lignes);
    }

    private void chargerFournisseurs() {
        fournisseurs = tiersDao.listerParType("F");
        String[] noms = new String[fournisseurs.size()];
        for (int i = 0; i < fournisseurs.size(); i++) noms[i] = fournisseurs.get(i).getTier();
        spinFournisseur.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, noms));
    }

    private void remplirEntete() {
        if (journeeExistante == null) return;
        etDate.setText(journeeExistante.getDate());
        etLibelle.setText(journeeExistante.getLibelle() != null ? journeeExistante.getLibelle() : "");
        etRemise.setText(String.valueOf(journeeExistante.getRemise()));
        etDec.setText(String.valueOf(journeeExistante.getDec()));
        long idT = journeeExistante.getIdTiers();
        for (int i = 0; i < fournisseurs.size(); i++) {
            if (fournisseurs.get(i).getId() == idT) { spinFournisseur.setSelection(i); break; }
        }
    }

    private void ajouterLigne() {
        String code = etCodeBarre.getText() != null ? etCodeBarre.getText().toString().trim() : "";
        String art  = etArt.getText()       != null ? etArt.getText().toString().trim()       : "";

        Article article = null;
        if (!code.isEmpty()) article = articleDao.parCodeBarre(code);
        if (article == null && !art.isEmpty()) article = articleDao.parArt(art);

        if (article == null) {
            // Proposer de créer l'article
            String ref = !code.isEmpty() ? code : art;
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
        if (prix <= 0) prix = article.getPa(); // prix achat par défaut

        JourneeDetail ligne = new JourneeDetail(article.getId(), article.getArt(), qte, prix, article.getUa());
        lignes.add(ligne);
        lignesAdapter.notifyItemInserted(lignes.size() - 1);

        // Réinitialiser champs article
        etCodeBarre.setText("");
        etArt.setText("");
        etQte.setText("1");
        etPrix.setText("");
        recalculer();
    }

    private void supprimerLigne(int pos) {
        lignes.remove(pos);
        lignesAdapter.notifyItemRemoved(pos);
        recalculer();
    }

    private void recalculer() {
        double total = 0;
        for (JourneeDetail d : lignes) total += d.getMontantTotal();
        double remise = FormatUtils.parseDouble(etRemise.getText() != null ? etRemise.getText().toString() : "0");
        double net    = total - remise;
        double dec    = FormatUtils.parseDouble(etDec.getText() != null ? etDec.getText().toString() : "0");
        double reste  = net - dec;
        tvTotal.setText("Total: " + FormatUtils.montant(total));
        tvNet.setText("Net: " + FormatUtils.montant(net));
        tvReste.setText(reste > 0 ? "Dette: " + FormatUtils.montant(reste) : "Réglé");
        tvReste.setTextColor(getColor(reste > 0 ? R.color.dette_color : R.color.achat_color));
    }

    private void enregistrer() {
        if (lignes.isEmpty()) {
            Toast.makeText(this, getString(R.string.aucun_article), Toast.LENGTH_SHORT).show();
            return;
        }
        Journee j = new Journee();
        j.setDate(etDate.getText() != null ? etDate.getText().toString() : FormatUtils.dateAujourdhui());
        j.setType(DatabaseHelper.TYPE_ACHAT);
        j.setLibelle(etLibelle.getText() != null ? etLibelle.getText().toString() : "");
        j.setDetails(lignes);
        j.recalculerMontant();
        double remise = FormatUtils.parseDouble(etRemise.getText() != null ? etRemise.getText().toString() : "0");
        double dec    = FormatUtils.parseDouble(etDec.getText() != null ? etDec.getText().toString() : "0");
        j.setRemise(remise);
        j.setDec(dec);
        j.setEnc(0);
        // Fournisseur
        int pos = spinFournisseur.getSelectedItemPosition();
        long idTiers = (pos >= 0 && pos < fournisseurs.size())
                ? fournisseurs.get(pos).getId()
                : DatabaseHelper.FOURN_COMPTANT_ID;
        j.setIdTiers(idTiers);

        // Si dec == net → comptant, sinon dette
        long result = journeeDao.enregistrerOperation(j, true);
        if (result > 0) {
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

    // ── Adapter lignes ───────────────────────────────────────
    static class LignesAdapter extends RecyclerView.Adapter<LignesAdapter.VH> {
        interface OnSuppr { void on(int pos); }
        private final List<JourneeDetail> data;
        private final OnSuppr onSuppr;
        LignesAdapter(List<JourneeDetail> d, OnSuppr s) { data = d; onSuppr = s; }

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

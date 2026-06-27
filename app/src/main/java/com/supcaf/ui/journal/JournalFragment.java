package com.supcaf.ui.journal;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.supcaf.R;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.dao.JourneeDao;
import com.supcaf.data.dao.TiersDao;
import com.supcaf.data.models.Journee;
import com.supcaf.data.models.Tiers;
import com.supcaf.utils.FormatUtils;
import java.util.List;

public class JournalFragment extends Fragment {

    private JourneeDao journeeDao;
    private TiersDao   tiersDao;
    private JournalAdapter adapter;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_journal, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        journeeDao = new JourneeDao(DatabaseHelper.getInstance(requireContext()));
        tiersDao   = new TiersDao(DatabaseHelper.getInstance(requireContext()));
        tvEmpty    = view.findViewById(R.id.tv_empty);

        RecyclerView rv = view.findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new JournalAdapter(this::onLongClick);
        rv.setAdapter(adapter);

        // FAB → menu opérations simples
        FloatingActionButton fab = view.findViewById(R.id.fab_nouveau);
        fab.setOnClickListener(v -> afficherMenuOperation());

        charger();
    }

    @Override public void onResume() { super.onResume(); charger(); }

    private void charger() {
        List<Journee> list = journeeDao.listerTout();
        adapter.setData(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void afficherMenuOperation() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Nouvelle opération")
            .setItems(new CharSequence[]{
                "Versement / Fournisseur (VF)",
                "Encaissement / Client (EC)",
                "Autres / Recettes (Entrée)",
                "Autres / Dépenses (Sortie)"
            }, (d, which) -> {
                switch (which) {
                    case 0: dialogSaisieSimple(DatabaseHelper.TYPE_VERSEMENT_F,  "Versement Fournisseur", "F"); break;
                    case 1: dialogSaisieSimple(DatabaseHelper.TYPE_ENCAISSEMENT, "Encaissement Client",   "C"); break;
                    case 2: dialogSaisieSimple(DatabaseHelper.TYPE_AUTRES,       "Autres / Recettes",     "AR"); break;
                    case 3: dialogSaisieSimple(DatabaseHelper.TYPE_AUTRES,       "Autres / Dépenses",     "AD"); break;
                }
            }).show();
    }

    private void dialogSaisieSimple(String type, String titre, String typeTiers) {
        // Charger la liste des tiers du bon type
        List<Tiers> tiersList = tiersDao.listerParType(
            "VF".equals(type) ? "F" : "C".equals(typeTiers) ? "C" : "A");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_saisie_simple, null);
        TextView tvTitre   = dialogView.findViewById(R.id.tv_titre_op);
        Spinner  spinTiers = dialogView.findViewById(R.id.spin_tiers);
        com.google.android.material.textfield.TextInputEditText etDate    = dialogView.findViewById(R.id.et_date);
        com.google.android.material.textfield.TextInputEditText etLibelle = dialogView.findViewById(R.id.et_libelle);
        com.google.android.material.textfield.TextInputEditText etMt      = dialogView.findViewById(R.id.et_mt);
        com.google.android.material.textfield.TextInputEditText etMtEnc   = dialogView.findViewById(R.id.et_mt_mouvement);
        TextView tvMtLabel = dialogView.findViewById(R.id.tv_mt_mouvement_label);

        tvTitre.setText(titre);
        etDate.setText(FormatUtils.dateAujourdhui());

        // Spinner tiers
        if (tiersList.isEmpty() || "A".equals(typeTiers)) {
            spinTiers.setVisibility(View.GONE);
        } else {
            String[] noms = new String[tiersList.size()];
            for (int i = 0; i < tiersList.size(); i++) noms[i] = tiersList.get(i).getTier();
            spinTiers.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, noms));
        }

        // Label montant décaissé/encaissé
        if (DatabaseHelper.TYPE_VERSEMENT_F.equals(type)) {
            tvMtLabel.setText("Montant décaissé");
        } else if (DatabaseHelper.TYPE_ENCAISSEMENT.equals(type)) {
            tvMtLabel.setText("Montant encaissé");
        } else if ("AR".equals(typeTiers)) {
            tvMtLabel.setText("Montant encaissé (Recette)");
        } else {
            tvMtLabel.setText("Montant décaissé (Dépense)");
        }

        new AlertDialog.Builder(requireContext())
            .setTitle(titre)
            .setView(dialogView)
            .setPositiveButton(R.string.enregistrer, (d, w) -> {
                double mt    = FormatUtils.parseDouble(etMt.getText() != null ? etMt.getText().toString() : "0");
                double mouv  = FormatUtils.parseDouble(etMtEnc.getText() != null ? etMtEnc.getText().toString() : "0");
                String date  = etDate.getText() != null ? etDate.getText().toString() : FormatUtils.dateAujourdhui();
                String lib   = etLibelle.getText() != null ? etLibelle.getText().toString() : titre;

                Journee j = new Journee();
                j.setDate(date);
                j.setType(type);
                j.setLibelle(lib.isEmpty() ? titre : lib);
                j.setMt(mt);
                j.setRemise(0);

                // Déterminer tiers et enc/dec selon le type
                long idTiers = DatabaseHelper.FOURN_COMPTANT_ID;
                if (DatabaseHelper.TYPE_VERSEMENT_F.equals(type)) {
                    // VF : décaissé = mouv (réduction de dette fournisseur)
                    j.setDec(mouv); j.setEnc(0);
                    int pos = spinTiers.getSelectedItemPosition();
                    idTiers = (!tiersList.isEmpty() && pos >= 0 && pos < tiersList.size())
                            ? tiersList.get(pos).getId()
                            : DatabaseHelper.FOURN_COMPTANT_ID;
                } else if (DatabaseHelper.TYPE_ENCAISSEMENT.equals(type)) {
                    // EC : encaissé = mouv (réduction de créance client)
                    j.setEnc(mouv); j.setDec(0);
                    int pos = spinTiers.getSelectedItemPosition();
                    idTiers = (!tiersList.isEmpty() && pos >= 0 && pos < tiersList.size())
                            ? tiersList.get(pos).getId()
                            : DatabaseHelper.CLIENT_COMPTANT_ID;
                } else if ("AR".equals(typeTiers)) {
                    // Recettes
                    j.setEnc(mouv); j.setDec(0);
                    idTiers = 3; // Autres
                } else {
                    // Dépenses
                    j.setEnc(0); j.setDec(mouv);
                    idTiers = 3; // Autres
                }
                j.setIdTiers(idTiers);

                long result = journeeDao.enregistrerSimple(j);
                if (result > 0) {
                    Toast.makeText(requireContext(), getString(R.string.enregistre), Toast.LENGTH_SHORT).show();
                    charger();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.erreur_saisie), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.annuler, null)
            .show();
    }

    private void onLongClick(Journee j) {
        new AlertDialog.Builder(requireContext())
            .setTitle(j.getTypeLabel() + " — " + FormatUtils.dateAffichage(j.getDate()))
            .setItems(new CharSequence[]{"Supprimer"}, (d, which) -> {
                new AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.confirm_supprimer))
                    .setPositiveButton(R.string.oui, (dd, ww) -> {
                        journeeDao.supprimer(j.getId());
                        charger();
                    })
                    .setNegativeButton(R.string.non, null).show();
            }).show();
    }

    // ── Adapter ──────────────────────────────────────────────
    static class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.VH> {
        interface OnLong { void on(Journee j); }
        private List<Journee> data;
        private final OnLong onLong;
        JournalAdapter(OnLong l) { onLong = l; }
        void setData(List<Journee> d) { data = d; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_journal, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Journee j = data.get(pos);
            h.tvDate.setText(FormatUtils.dateAffichage(j.getDate()));
            h.tvType.setText(j.getTypeLabel());
            String lib = j.getLibelle() != null && !j.getLibelle().isEmpty()
                    ? j.getLibelle()
                    : (j.getNomTiers() != null ? j.getNomTiers() : "");
            h.tvLib.setText(lib);
            h.tvMt.setText(FormatUtils.montant(j.getNet()));

            // Couleur badge selon type
            int bgColor;
            switch (j.getType() != null ? j.getType() : "") {
                case "A":  bgColor = R.color.achat_color; break;
                case "V":  bgColor = R.color.vente_color; break;
                case "VF": bgColor = R.color.versement_color; break;
                case "EC": bgColor = R.color.encaissement_color; break;
                default:   bgColor = R.color.autres_color;
            }
            h.tvType.setBackgroundColor(h.itemView.getContext().getColor(bgColor));
            h.itemView.setOnLongClickListener(v -> { onLong.on(j); return true; });
        }

        @Override public int getItemCount() { return data == null ? 0 : data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvType, tvLib, tvMt;
            VH(View v) {
                super(v);
                tvDate = v.findViewById(R.id.tv_date);
                tvType = v.findViewById(R.id.tv_type);
                tvLib  = v.findViewById(R.id.tv_lib);
                tvMt   = v.findViewById(R.id.tv_mt);
            }
        }
    }
}

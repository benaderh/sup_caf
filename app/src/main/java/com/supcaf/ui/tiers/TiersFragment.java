package com.supcaf.ui.tiers;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.supcaf.R;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.dao.TiersDao;
import com.supcaf.data.models.Tiers;
import com.supcaf.utils.FormatUtils;
import java.util.List;

public class TiersFragment extends Fragment {

    private TiersDao dao;
    private TiersAdapter adapter;
    private TextView tvEmpty;
    private String typeActif = "F";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_tiers, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dao     = new TiersDao(DatabaseHelper.getInstance(requireContext()));
        tvEmpty = view.findViewById(R.id.tv_empty);

        RecyclerView rv = view.findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TiersAdapter(this::onTiersClick, this::onTiersLongClick);
        rv.setAdapter(adapter);

        TabLayout tabs = view.findViewById(R.id.tab_tiers);
        tabs.addTab(tabs.newTab().setText("Fournisseurs"));
        tabs.addTab(tabs.newTab().setText("Clients"));
        tabs.addTab(tabs.newTab().setText("Autres"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: typeActif = "F"; break;
                    case 1: typeActif = "C"; break;
                    case 2: typeActif = "A"; break;
                }
                charger();
            }
            public void onTabUnselected(TabLayout.Tab tab) {}
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        EditText etRech = view.findViewById(R.id.et_recherche);
        if (etRech != null) etRech.setVisibility(View.GONE); // pas de recherche sur tiers

        FloatingActionButton fab = view.findViewById(R.id.fab_nouveau);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TiersSaisieActivity.class);
            intent.putExtra("type_defaut", typeActif);
            startActivity(intent);
        });

        charger();
    }

    @Override public void onResume() { super.onResume(); charger(); }

    private void charger() {
        List<Tiers> list;
        switch (typeActif) {
            case "C":  list = dao.listerClients(); break;
            case "A":  list = dao.listerAutres(); break;
            default:   list = dao.listerFournisseurs();
        }
        adapter.setData(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onTiersClick(Tiers t) {
        Intent intent = new Intent(requireContext(), TiersDetailActivity.class);
        intent.putExtra("tiers_id", t.getId());
        startActivity(intent);
    }

    private void onTiersLongClick(Tiers t) {
        if (t.getId() <= 3) {
            Toast.makeText(requireContext(), "Tiers par défaut — non modifiable/supprimable", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
            .setTitle(t.getTier())
            .setItems(new CharSequence[]{"Modifier", "Supprimer"}, (d, which) -> {
                if (which == 0) {
                    Intent intent = new Intent(requireContext(), TiersSaisieActivity.class);
                    intent.putExtra("tiers_id", t.getId());
                    startActivity(intent);
                } else {
                    new AlertDialog.Builder(requireContext())
                        .setMessage(getString(R.string.confirm_supprimer))
                        .setPositiveButton(R.string.oui, (dd, ww) -> {
                            dao.supprimer(t.getId());
                            charger();
                        })
                        .setNegativeButton(R.string.non, null).show();
                }
            }).show();
    }

    // ── Adapter ──────────────────────────────────────────────
    static class TiersAdapter extends RecyclerView.Adapter<TiersAdapter.VH> {
        interface OnClick { void on(Tiers t); }
        private List<Tiers> data;
        private final OnClick onClick, onLongClick;

        TiersAdapter(OnClick c, OnClick lc) { this.onClick = c; this.onLongClick = lc; }
        void setData(List<Tiers> list) { this.data = list; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_tiers, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Tiers t = data.get(pos);
            h.tvNom.setText(t.getTier());
            h.tvType.setText(t.getTypeLabel());
            double s = t.getSolde();
            if ("F".equals(t.getType())) {
                h.tvSolde.setText(s > 0 ? "Dette: " + FormatUtils.montant(s) : "Soldé");
                h.tvSolde.setTextColor(h.itemView.getContext().getColor(
                    s > 0 ? R.color.dette_color : R.color.achat_color));
            } else if ("C".equals(t.getType())) {
                h.tvSolde.setText(s > 0 ? "Créance: " + FormatUtils.montant(s) : "Soldé");
                h.tvSolde.setTextColor(h.itemView.getContext().getColor(
                    s > 0 ? R.color.creance_color : R.color.achat_color));
            } else {
                h.tvSolde.setText("");
            }
            h.itemView.setOnClickListener(v -> onClick.on(t));
            h.itemView.setOnLongClickListener(v -> { onLongClick.on(t); return true; });
        }

        @Override public int getItemCount() { return data == null ? 0 : data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvNom, tvType, tvSolde;
            VH(View v) {
                super(v);
                tvNom   = v.findViewById(R.id.tv_nom);
                tvType  = v.findViewById(R.id.tv_type);
                tvSolde = v.findViewById(R.id.tv_solde);
            }
        }
    }
}

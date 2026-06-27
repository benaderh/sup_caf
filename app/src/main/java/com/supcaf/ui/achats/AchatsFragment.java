package com.supcaf.ui.achats;

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
import com.supcaf.R;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.dao.JourneeDao;
import com.supcaf.data.models.Journee;
import com.supcaf.utils.FormatUtils;
import java.util.List;

public class AchatsFragment extends Fragment {

    private JourneeDao dao;
    private AchatAdapter adapter;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_liste_generic, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dao     = new JourneeDao(DatabaseHelper.getInstance(requireContext()));
        tvEmpty = view.findViewById(R.id.tv_empty);

        RecyclerView rv = view.findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AchatAdapter(this::onClick, this::onLongClick);
        rv.setAdapter(adapter);

        EditText etRech = view.findViewById(R.id.et_recherche);
        etRech.setVisibility(View.GONE);

        FloatingActionButton fab = view.findViewById(R.id.fab_nouveau);
        fab.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), AchatSaisieActivity.class)));

        charger();
    }

    @Override public void onResume() { super.onResume(); charger(); }

    private void charger() {
        List<Journee> list = dao.listerAchats();
        adapter.setData(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onClick(Journee j) {
        Intent intent = new Intent(requireContext(), AchatSaisieActivity.class);
        intent.putExtra("journee_id", j.getId());
        startActivity(intent);
    }

    private void onLongClick(Journee j) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Achat du " + FormatUtils.dateAffichage(j.getDate()))
            .setItems(new CharSequence[]{"Voir détail", "Supprimer"}, (d, which) -> {
                if (which == 0) onClick(j);
                else {
                    new AlertDialog.Builder(requireContext())
                        .setMessage(getString(R.string.confirm_supprimer))
                        .setPositiveButton(R.string.oui, (dd, ww) -> {
                            dao.supprimer(j.getId());
                            charger();
                        })
                        .setNegativeButton(R.string.non, null).show();
                }
            }).show();
    }

    // ── Adapter ──────────────────────────────────────────────
    static class AchatAdapter extends RecyclerView.Adapter<AchatAdapter.VH> {
        interface OnJ { void on(Journee j); }
        private List<Journee> data;
        private final OnJ onClick, onLong;
        AchatAdapter(OnJ c, OnJ l) { onClick = c; onLong = l; }
        void setData(List<Journee> d) { data = d; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_journee, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Journee j = data.get(pos);
            h.tvDate.setText(FormatUtils.dateAffichage(j.getDate()));
            h.tvTiers.setText(j.getNomTiers() != null ? j.getNomTiers() : "Comptant");
            h.tvMt.setText(FormatUtils.montant(j.getNet()));
            double reste = j.getReste();
            h.tvReste.setText(reste > 0 ? "Dette: " + FormatUtils.montant(reste) : "Réglé");
            h.tvReste.setTextColor(h.itemView.getContext().getColor(
                reste > 0 ? R.color.dette_color : R.color.achat_color));
            h.tvBadge.setText("ACHAT");
            h.tvBadge.setBackgroundColor(h.itemView.getContext().getColor(R.color.achat_color));
            h.itemView.setOnClickListener(v -> onClick.on(j));
            h.itemView.setOnLongClickListener(v -> { onLong.on(j); return true; });
        }

        @Override public int getItemCount() { return data == null ? 0 : data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvTiers, tvMt, tvReste, tvBadge;
            VH(View v) {
                super(v);
                tvDate  = v.findViewById(R.id.tv_date);
                tvTiers = v.findViewById(R.id.tv_tiers);
                tvMt    = v.findViewById(R.id.tv_mt);
                tvReste = v.findViewById(R.id.tv_reste);
                tvBadge = v.findViewById(R.id.tv_badge);
            }
        }
    }
}

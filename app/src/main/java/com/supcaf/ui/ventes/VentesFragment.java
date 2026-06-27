package com.supcaf.ui.ventes;

import android.content.Intent;
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
import com.supcaf.data.models.Journee;
import com.supcaf.utils.FormatUtils;
import java.util.List;

public class VentesFragment extends Fragment {

    private JourneeDao dao;
    private VenteAdapter adapter;
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
        adapter = new VenteAdapter(this::onClick, this::onLongClick);
        rv.setAdapter(adapter);

        view.findViewById(R.id.et_recherche).setVisibility(View.GONE);

        FloatingActionButton fab = view.findViewById(R.id.fab_nouveau);
        fab.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), VenteSaisieActivity.class)));

        charger();
    }

    @Override public void onResume() { super.onResume(); charger(); }

    private void charger() {
        List<Journee> list = dao.listerVentes();
        adapter.setData(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onClick(Journee j) {
        Intent intent = new Intent(requireContext(), VenteSaisieActivity.class);
        intent.putExtra("journee_id", j.getId());
        startActivity(intent);
    }

    private void onLongClick(Journee j) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Vente du " + FormatUtils.dateAffichage(j.getDate()))
            .setItems(new CharSequence[]{"Voir détail", "Supprimer"}, (d, which) -> {
                if (which == 0) onClick(j);
                else new AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.confirm_supprimer))
                    .setPositiveButton(R.string.oui, (dd, ww) -> { dao.supprimer(j.getId()); charger(); })
                    .setNegativeButton(R.string.non, null).show();
            }).show();
    }

    static class VenteAdapter extends RecyclerView.Adapter<VenteAdapter.VH> {
        interface OnJ { void on(Journee j); }
        private List<Journee> data;
        private final OnJ onClick, onLong;
        VenteAdapter(OnJ c, OnJ l) { onClick = c; onLong = l; }
        void setData(List<Journee> d) { data = d; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_journee, p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Journee j = data.get(pos);
            h.tvDate.setText(FormatUtils.dateAffichage(j.getDate()));
            h.tvTiers.setText(j.getNomTiers() != null ? j.getNomTiers() : "Comptant");
            h.tvMt.setText(FormatUtils.montant(j.getNet()));
            double reste = j.getReste();
            h.tvReste.setText(reste > 0 ? "Créance: " + FormatUtils.montant(reste) : "Encaissé");
            h.tvReste.setTextColor(ContextCompat.getColor(h.itemView.getContext(), 
                reste > 0 ? R.color.creance_color : R.color.vente_color));
            h.tvBadge.setText("VENTE");
            h.tvBadge.setBackgroundColor(ContextCompat.getColor(h.itemView.getContext(), R.color.vente_color));
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

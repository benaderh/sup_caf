package com.supcaf.ui.tiers;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.*;
import com.supcaf.R;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.data.dao.JourneeDao;
import com.supcaf.data.dao.TiersDao;
import com.supcaf.data.models.Journee;
import com.supcaf.data.models.Tiers;
import com.supcaf.utils.FormatUtils;
import java.util.List;

public class TiersDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiers_detail);
        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        long tiersId = getIntent().getLongExtra("tiers_id", -1);
        if (tiersId < 0) { finish(); return; }

        TiersDao tiersDao = new TiersDao(DatabaseHelper.getInstance(this));
        JourneeDao jDao   = new JourneeDao(DatabaseHelper.getInstance(this));
        Tiers t = tiersDao.parId(tiersId);
        if (t == null) { finish(); return; }

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(t.getTier());

        TextView tvType  = findViewById(R.id.tv_type);
        TextView tvSolde = findViewById(R.id.tv_solde_label);
        TextView tvMt    = findViewById(R.id.tv_mt_solde);

        tvType.setText(t.getTypeLabel());
        double solde = tiersDao.getSolde(tiersId, t.getType());

        if ("F".equals(t.getType())) {
            tvSolde.setText("Dette fournisseur :");
            tvMt.setTextColor(getColor(solde > 0 ? R.color.dette_color : R.color.achat_color));
        } else if ("C".equals(t.getType())) {
            tvSolde.setText("Créance client :");
            tvMt.setTextColor(getColor(solde > 0 ? R.color.creance_color : R.color.achat_color));
        } else {
            tvSolde.setText("Solde :");
        }
        tvMt.setText(FormatUtils.montant(solde));

        // Liste des mouvements
        List<Journee> mouvements = jDao.listerParTiers(tiersId);
        RecyclerView rv = findViewById(R.id.rv_mouvements);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new MouvAdapter(mouvements));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ── Adapter mouvements ───────────────────────────────────
    static class MouvAdapter extends RecyclerView.Adapter<MouvAdapter.VH> {
        private final List<Journee> data;
        MouvAdapter(List<Journee> d) { this.data = d; }

        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            return new VH(android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_mouvement, p, false));
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            Journee j = data.get(pos);
            h.tvDate.setText(FormatUtils.dateAffichage(j.getDate()));
            h.tvLib.setText(j.getTypeLabel() + (j.getLibelle() != null ? " — " + j.getLibelle() : ""));
            h.tvNet.setText("Net: " + FormatUtils.montant(j.getNet()));
            double reste = j.getReste();
            h.tvReste.setText(reste > 0 ? "Reste: " + FormatUtils.montant(reste) : "Réglé");
            h.tvReste.setTextColor(h.itemView.getContext().getColor(
                reste > 0 ? R.color.dette_color : R.color.achat_color));
        }

        @Override public int getItemCount() { return data == null ? 0 : data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvLib, tvNet, tvReste;
            VH(android.view.View v) {
                super(v);
                tvDate  = v.findViewById(R.id.tv_date);
                tvLib   = v.findViewById(R.id.tv_libelle);
                tvNet   = v.findViewById(R.id.tv_net);
                tvReste = v.findViewById(R.id.tv_reste);
            }
        }
    }
}

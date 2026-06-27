package com.supcaf.ui.dashboard;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.supcaf.R;
import com.supcaf.data.DatabaseHelper;
import com.supcaf.utils.FormatUtils;

public class DashboardFragment extends Fragment {

    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = DatabaseHelper.getInstance(requireContext());
        chargerTableauDeBord(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) chargerTableauDeBord(getView());
    }

    private void chargerTableauDeBord(View view) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String today = FormatUtils.dateAujourdhui();

        // ── Solde caisse = SUM(enc) - SUM(dec) toutes opérations ──
        double totalEnc = queryDouble(db,
                "SELECT COALESCE(SUM(j_enc),0) FROM journee");
        double totalDec = queryDouble(db,
                "SELECT COALESCE(SUM(j_dec),0) FROM journee");
        double soldeCaisse = totalEnc - totalDec;

        // ── Dettes fournisseurs non réglées ──
        // dette = SUM(j_mt - j_remise - j_dec) pour j_type='A' où id_t != 1
        double dettes = queryDouble(db,
                "SELECT COALESCE(SUM(j_mt - j_remise - j_dec), 0) " +
                "FROM journee WHERE j_type='A' AND id_t != 1 " +
                "AND (j_mt - j_remise - j_dec) > 0");

        // Versements fournisseurs reçus en trop (EC sur fournisseurs = réduction dette)
        double versementsF = queryDouble(db,
                "SELECT COALESCE(SUM(j_dec),0) FROM journee WHERE j_type='VF'");
        // La dette nette tient déjà compte de j_dec dans les achats
        // Les VF supplémentaires sont des règlements directs — déjà dans j_dec des achats non
        // Calcul correct : dette par tiers = SUM(net_achat) - SUM(dec_achats + VF pour ce tiers)

        // ── Créances clients non réglées ──
        double creances = queryDouble(db,
                "SELECT COALESCE(SUM(j_mt - j_remise - j_enc), 0) " +
                "FROM journee WHERE j_type='V' AND id_t != 2 " +
                "AND (j_mt - j_remise - j_enc) > 0");

        // ── Ventes du jour ──
        double ventesJour = queryDouble(db,
                "SELECT COALESCE(SUM(j_mt - j_remise),0) FROM journee " +
                "WHERE j_type='V' AND j_date=?", today);

        // ── Achats du jour ──
        double achatsJour = queryDouble(db,
                "SELECT COALESCE(SUM(j_mt - j_remise),0) FROM journee " +
                "WHERE j_type='A' AND j_date=?", today);

        // ── Nb articles en rupture (qs <= 0) ──
        long rupture = queryLong(db,
                "SELECT COUNT(*) FROM articles WHERE qs <= 0");

        // ── Affichage ──
        setText(view, R.id.tv_solde_caisse,  FormatUtils.montant(soldeCaisse));
        setText(view, R.id.tv_dettes,        FormatUtils.montant(dettes));
        setText(view, R.id.tv_creances,      FormatUtils.montant(creances));
        setText(view, R.id.tv_ventes_jour,   FormatUtils.montant(ventesJour));
        setText(view, R.id.tv_achats_jour,   FormatUtils.montant(achatsJour));
        setText(view, R.id.tv_rupture,       rupture + " article(s)");
        setText(view, R.id.tv_date_jour,     "Aujourd'hui : " + FormatUtils.dateAffichage(today));

        // Couleur solde caisse
        TextView tvCaisse = view.findViewById(R.id.tv_solde_caisse);
        if (tvCaisse != null) {
            tvCaisse.setTextColor(soldeCaisse >= 0
                    ? getResources().getColor(R.color.caisse_color, null)
                    : getResources().getColor(R.color.dette_color, null));
        }
    }

    private void setText(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private double queryDouble(SQLiteDatabase db, String sql, String... args) {
        try (Cursor c = db.rawQuery(sql, args.length > 0 ? args : null)) {
            if (c.moveToFirst()) return c.getDouble(0);
        }
        return 0.0;
    }

    private long queryLong(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql, null)) {
            if (c.moveToFirst()) return c.getLong(0);
        }
        return 0L;
    }
}

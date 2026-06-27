package com.supcaf.ui.articles;

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
import com.supcaf.data.dao.ArticleDao;
import com.supcaf.data.models.Article;
import com.supcaf.utils.FormatUtils;
import java.util.List;

public class ArticlesFragment extends Fragment {

    private ArticleDao dao;
    private ArticleAdapter adapter;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_liste_generic, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dao = new ArticleDao(DatabaseHelper.getInstance(requireContext()));
        tvEmpty = view.findViewById(R.id.tv_empty);

        RecyclerView rv = view.findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ArticleAdapter(this::onArticleClick, this::onArticleLongClick);
        rv.setAdapter(adapter);

        EditText etRecherche = view.findViewById(R.id.et_recherche);
        etRecherche.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { charger(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });

        FloatingActionButton fab = view.findViewById(R.id.fab_nouveau);
        fab.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), ArticleSaisieActivity.class)));

        charger("");
    }

    @Override public void onResume() { super.onResume(); charger(""); }

    private void charger(String terme) {
        List<Article> list = terme.isEmpty() ? dao.listerTous() : dao.rechercher(terme);
        adapter.setData(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onArticleClick(Article a) {
        Intent intent = new Intent(requireContext(), ArticleSaisieActivity.class);
        intent.putExtra("article_id", a.getId());
        startActivity(intent);
    }

    private void onArticleLongClick(Article a) {
        new AlertDialog.Builder(requireContext())
            .setTitle(a.getArt())
            .setItems(new CharSequence[]{"Modifier", "Supprimer"}, (d, which) -> {
                if (which == 0) onArticleClick(a);
                else confirmerSuppression(a);
            }).show();
    }

    private void confirmerSuppression(Article a) {
        new AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.confirm_supprimer))
            .setPositiveButton(R.string.oui, (d, w) -> {
                dao.supprimer(a.getId());
                charger("");
                Toast.makeText(requireContext(), "Supprimé", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.non, null).show();
    }

    static class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.VH> {
        interface OnClick { void on(Article a); }
        private List<Article> data;
        private final OnClick onClick, onLongClick;

        ArticleAdapter(OnClick c, OnClick lc) { this.onClick = c; this.onLongClick = lc; }
        void setData(List<Article> list) { this.data = list; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_article, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Article a = data.get(pos);
            h.tvArt.setText(a.getArt());
            h.tvPv.setText("PV: " + FormatUtils.montant(a.getPv()) + " / " + a.getUv());
            h.tvStock.setText("Stock: " + FormatUtils.quantite(a.getQs()) + " " + a.getUv());
            h.tvCat.setText(a.getNomCategorie() != null ? a.getNomCategorie() : "");
            h.tvStock.setTextColor(ContextCompat.getColor(h.itemView.getContext(), 
                a.getQs() <= 0 ? R.color.dette_color : R.color.text_secondary));
            h.itemView.setOnClickListener(v -> onClick.on(a));
            h.itemView.setOnLongClickListener(v -> { onLongClick.on(a); return true; });
        }

        @Override public int getItemCount() { return data == null ? 0 : data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvArt, tvPv, tvStock, tvCat;
            VH(View v) {
                super(v);
                tvArt   = v.findViewById(R.id.tv_art);
                tvPv    = v.findViewById(R.id.tv_pv);
                tvStock = v.findViewById(R.id.tv_stock);
                tvCat   = v.findViewById(R.id.tv_cat);
            }
        }
    }
}

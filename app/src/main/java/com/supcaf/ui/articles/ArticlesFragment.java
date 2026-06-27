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
import com.supcaf.data.dao.CategorieDao;
import com.supcaf.data.models.Article;
import com.supcaf.data.models.Categorie;
import com.supcaf.utils.FormatUtils;
import java.util.List;
import com.google.android.material.tabs.TabLayout;

public class ArticlesFragment extends Fragment {

    private ArticleDao articleDao;
    private CategorieDao categorieDao;
    private ArticleAdapter articleAdapter;
    private CategorieAdapter categorieAdapter;
    private TextView tvEmpty;
    private RecyclerView rv;
    private FloatingActionButton fab;
    private boolean isArticleTab = true;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_articles_tabs, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        articleDao = new ArticleDao(DatabaseHelper.getInstance(requireContext()));
        categorieDao = new CategorieDao(DatabaseHelper.getInstance(requireContext()));
        
        tvEmpty = view.findViewById(R.id.tv_empty);
        rv = view.findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        articleAdapter = new ArticleAdapter(this::onArticleClick, this::onArticleLongClick);
        categorieAdapter = new CategorieAdapter();
        categorieAdapter.setListener(new CategorieAdapter.OnCategorieClickListener() {
            @Override public void onClick(Categorie c) { /* Optionnel: filtrer les articles ? */ }
            @Override public void onLongClick(Categorie c) { onCategorieLongClick(c); }
        });
        
        rv.setAdapter(articleAdapter);

        EditText etRecherche = view.findViewById(R.id.et_recherche);
        etRecherche.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { charger(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });

        fab = view.findViewById(R.id.fab_nouveau);
        fab.setOnClickListener(v -> {
            if (isArticleTab) {
                startActivity(new Intent(requireContext(), ArticleSaisieActivity.class));
            } else {
                dialogSaisieCategorie(null);
            }
        });

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText("Articles"));
            tabLayout.addTab(tabLayout.newTab().setText("Catégories"));
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    isArticleTab = (tab.getPosition() == 0);
                    rv.setAdapter(isArticleTab ? articleAdapter : categorieAdapter);
                    charger(etRecherche.getText() != null ? etRecherche.getText().toString() : "");
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
        }

        charger("");
    }

    @Override public void onResume() { super.onResume(); charger(""); }

    private void charger(String terme) {
        if (isArticleTab) {
            List<Article> list = terme.isEmpty() ? articleDao.listerTous() : articleDao.rechercher(terme);
            articleAdapter.setData(list);
            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            List<Categorie> list = categorieDao.listerToutes();
            categorieAdapter.setCategories(list);
            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        }
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
                int res = articleDao.supprimer(a.getId());
                if (res == -2) Toast.makeText(requireContext(), "Impossible : Utilisé", Toast.LENGTH_SHORT).show();
                charger("");
            })
            .setNegativeButton(R.string.non, null).show();
    }

    // --- Catégories ---
    private void onCategorieLongClick(Categorie c) {
        new AlertDialog.Builder(requireContext())
            .setTitle(c.getCategorie())
            .setItems(new CharSequence[]{"Modifier", "Supprimer"}, (d, which) -> {
                if (which == 0) dialogSaisieCategorie(c);
                else {
                    new AlertDialog.Builder(requireContext())
                        .setMessage("Supprimer cette catégorie ?")
                        .setPositiveButton(R.string.oui, (d2, w2) -> {
                            int res = categorieDao.supprimer(c.getId());
                            if (res == -2) Toast.makeText(requireContext(), "Impossible : Catégorie utilisée", Toast.LENGTH_SHORT).show();
                            charger("");
                        }).setNegativeButton(R.string.non, null).show();
                }
            }).show();
    }

    private void dialogSaisieCategorie(@Nullable Categorie c) {
        EditText et = new EditText(requireContext());
        if (c != null) et.setText(c.getCategorie());
        new AlertDialog.Builder(requireContext())
            .setTitle(c == null ? "Nouvelle catégorie" : "Modifier catégorie")
            .setView(et)
            .setPositiveButton(R.string.enregistrer, (d, w) -> {
                String nom = et.getText().toString().trim();
                if (!nom.isEmpty()) {
                    if (c == null) categorieDao.inserer(nom);
                    else {
                        // Pas de update dans DAO ? S'il n'y a pas, on gère sinon on ajoute
                        android.content.ContentValues cv = new android.content.ContentValues();
                        cv.put(DatabaseHelper.CAT_CAT, nom);
                        DatabaseHelper.getInstance(requireContext()).getWritableDatabase()
                            .update(DatabaseHelper.T_CATEGORIE, cv, DatabaseHelper.CAT_ID + "=?", new String[]{String.valueOf(c.getId())});
                    }
                    charger("");
                }
            })
            .setNegativeButton(R.string.annuler, null).show();
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
            h.tvStock.setTextColor(h.itemView.getContext().getColor(
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

package com.supcaf.ui.articles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.supcaf.R;
import com.supcaf.data.models.Categorie;
import java.util.ArrayList;
import java.util.List;

public class CategorieAdapter extends RecyclerView.Adapter<CategorieAdapter.CategorieVH> {

    public interface OnCategorieClickListener {
        void onClick(Categorie c);
        void onLongClick(Categorie c);
    }

    private List<Categorie> categories = new ArrayList<>();
    private OnCategorieClickListener listener;

    public void setCategories(List<Categorie> list) {
        this.categories = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setListener(OnCategorieClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public CategorieVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new CategorieVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CategorieVH holder, int position) {
        Categorie c = categories.get(position);
        holder.tvNom.setText(c.getCat());
        holder.tvObs.setText(c.getObs() != null ? c.getObs() : "");
        final long[] lastClickTime = {0};
        holder.itemView.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime[0] < 300) {
                if (listener != null) listener.onClick(c);
            }
            lastClickTime[0] = clickTime;
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(c);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategorieVH extends RecyclerView.ViewHolder {
        TextView tvNom, tvObs;
        CategorieVH(View v) {
            super(v);
            tvNom = v.findViewById(R.id.tv_art);
            tvObs = v.findViewById(R.id.tv_cat);
            TextView tvPv = v.findViewById(R.id.tv_pv);
            TextView tvStock = v.findViewById(R.id.tv_stock);
            tvPv.setText("");
            tvStock.setText("");
        }
    }
}

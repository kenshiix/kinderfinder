package com.example.kinderfind.adapters;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kinderfind.R;
import com.example.kinderfind.activities.InformationActivity;
import com.example.kinderfind.models.Kindergarten;
import java.util.ArrayList;
import java.util.Collections;

import android.widget.Filter;
import android.widget.Filterable;

public class KindergartenAdapter extends RecyclerView.Adapter <KindergartenAdapter.KindergartenHolder> implements Filterable {

    private Activity activity;
    private ArrayList<Kindergarten> filteredArrayList = new ArrayList<>();
    private ArrayList<Kindergarten> originalArrayList = new ArrayList<>();
    private ItemFilter mFilter = new ItemFilter();

    public KindergartenAdapter(ArrayList<Kindergarten> arrayList, Activity activity){
        this.activity = activity;
        this.originalArrayList = arrayList;
        this.filteredArrayList = arrayList;
    }

    @NonNull
    @Override
    public KindergartenHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.kindergarten_item, parent, false);
        return new KindergartenHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull KindergartenHolder holder, int position) {

        Kindergarten currentKindergarten = filteredArrayList.get(position);
        holder.textViewName.setText(currentKindergarten.getCentre_name());
        holder.textViewAdd.setText((currentKindergarten.getCenter_address()));
        holder.textViewDist.setText(String.format("%.1fkm", currentKindergarten.getDistance() / 1000));

    }

    @Override
    public int getItemCount() {
        if (filteredArrayList == null)
            return 0;

        return filteredArrayList.size();
    }

    public void setKindergartens(ArrayList<Kindergarten> kindergartens){
        this.originalArrayList = kindergartens;
        this.filteredArrayList = kindergartens;
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    class KindergartenHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView textViewName;
        private TextView textViewAdd;
        private TextView textViewDist;

        public KindergartenHolder(@NonNull View itemView) {
            super(itemView);

            textViewName = itemView.findViewById(R.id.kindergarten_name_tv);
            textViewAdd = itemView.findViewById(R.id.kindergarten_add_tv);
            textViewDist = itemView.findViewById(R.id.kindergarten_dist_tv);
            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View v) {
            InformationActivity.kindergarten = originalArrayList.get(getAdapterPosition());
            activity.startActivity(new Intent(activity, InformationActivity.class));
        }
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String filterString = constraint.toString().toLowerCase();
            FilterResults results = new FilterResults();
            ArrayList<Kindergarten> matches = new ArrayList<>();

            if (filterString.equalsIgnoreCase(""))
            {
                results.values = originalArrayList;
                results.count = originalArrayList.size();
            }
            else
            {
                for (int i = 0; i < originalArrayList.size(); i++) {
                    String name, address;

                    if (originalArrayList.get(i).getCentre_name() == null)
                        name = "";
                    else
                        name = originalArrayList.get(i).getCentre_name().toLowerCase();

                    if (originalArrayList.get(i).getCenter_address() == null)
                        address = "";
                    else
                        address = originalArrayList.get(i).getCenter_address().toLowerCase();

                    if (name.contains(filterString) || address.contains(filterString)) {
                        matches.add(originalArrayList.get(i));
                    }
                }

                results.values = matches;
                results.count = matches.size();

            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredArrayList = (ArrayList<Kindergarten>) results.values;
            notifyDataSetChanged();
        }

    }

}

package com.dsvl0.preferenseseditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class AppCardAdapter extends RecyclerView.Adapter<AppCardAdapter.AppViewHolder> {

    private final List<SystemAppInfo> appList;
    private final Context context;
    private OnAppClickListener listener;

    public AppCardAdapter(Context context) {
        this.context = context;
        this.appList = new ArrayList<>();
    }

    public class AppViewHolder extends RecyclerView.ViewHolder {
        TextView appName;
        TextView packageName;
        ImageView appIcon;

        public AppViewHolder(View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.AppName);
            packageName = itemView.findViewById(R.id.packageName);
            appIcon = itemView.findViewById(R.id.AppIcon);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAbsoluteAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onAppClick(appList.get(position));
                    }
                }
            });

        }

        public void bind(SystemAppInfo appCard) {
            appName.setText(appCard.getName());
            packageName.setText(appCard.getPackageName());
            appIcon.setImageBitmap(appCard.getIcon());

            Glide.with(context)
                    .load(appCard.getIcon())
                    .circleCrop()
                    .into(appIcon);
        }
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.appcard, parent, false);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).start();
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        holder.bind(appList.get(position));
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public void addApp(SystemAppInfo appCard) {
        appList.add(appCard);
        notifyItemInserted(appList.size() - 1);
    }

    public void clearApps() {
        final int size = appList.size();
        appList.clear();
        notifyItemRangeRemoved(0, size);
    }

    public interface OnAppClickListener {
        void onAppClick(SystemAppInfo app);
    }

    public void setOnAppClickListener(OnAppClickListener listener) {
        this.listener = listener;
    }


}


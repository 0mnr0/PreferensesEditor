package com.dsvl0.preferenseseditor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class XmlFileAdapter extends RecyclerView.Adapter<XmlFileAdapter.FileViewHolder> {

    private final List<String> fileList = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(String fileName);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    // Добавление одного файла
    public void addFile(String fileName) {
        fileList.add(fileName);
        notifyItemInserted(fileList.size() - 1);
    }

    public void clearFiles() {
        fileList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.xml_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        String fileName = fileList.get(position);
        holder.lfilename.setText(fileName);
        holder.itemLayout.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(fileName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView lfilename;
        View itemLayout;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            lfilename = itemView.findViewById(R.id.xmlfilename);
            itemLayout = itemView.findViewById(R.id.xmlLayout);
        }
    }
}

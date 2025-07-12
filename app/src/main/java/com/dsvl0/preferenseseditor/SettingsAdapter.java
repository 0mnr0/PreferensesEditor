package com.dsvl0.preferenseseditor;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder> {

    private final List<SettingItem> settings;

    public SettingsAdapter(List<SettingItem> settings) {
        this.settings = settings;
    }

    @NonNull
    @Override
    public SettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.setting_edit_layout, parent, false);
        return new SettingsViewHolder(view);
    }



    // Метод для безопасной установки большого текста
    public void insertTextChunked(EditText editText, String fullText) {
        Handler handler = new Handler(Looper.getMainLooper());
        int chunkSize = 500;
        if (fullText.length() > 10000) {
            chunkSize = 3000;
        }
        if (fullText.length() > 100000) {
            chunkSize = 30000;
        }
        if (fullText.length() > 500000) {
            chunkSize = 100000;
        }
        final int delay = 200; // миллисекунд
        final int[] index = {0}; // текущее положение

        int finalChunkSize = chunkSize;
        Runnable inserter = new Runnable() {
            @Override
            public void run() {
                if (index[0] < fullText.length()) {
                    int end = Math.min(index[0] + finalChunkSize, fullText.length());
                    String chunk = fullText.substring(index[0], end);

                    editText.append(chunk);
                    index[0] = end;

                    handler.postDelayed(this, delay); // следующее добавление
                }
            }
        };

        handler.post(inserter); // запускаем первую вставку
    }



    @Override
    public void onBindViewHolder(@NonNull SettingsViewHolder holder, int position) {
        SettingItem item = settings.get(position);
        holder.VarType.setText(item.settingType);


        // Сначала скрываем оба блока
        holder.booleanLayout.setVisibility(View.GONE);
        holder.stringLayout.setVisibility(View.GONE);
        holder.intLayout.setVisibility(View.GONE);

        if ("boolean".equalsIgnoreCase(item.settingType)) {
            holder.booleanLayout.setVisibility(View.VISIBLE);
            holder.materialSwitch.setText(item.settingName);


            boolean checked = false;
            if (item.value instanceof Boolean) {
                checked = (Boolean) item.value;
            } else if (item.value instanceof String) {
                checked = Boolean.parseBoolean((String) item.value);
            }
            holder.materialSwitch.setChecked(checked);

        } else if ("string".equalsIgnoreCase(item.settingType)) {
            holder.stringLayout.setVisibility(View.VISIBLE);
            holder.textInputLayout.setHint(item.settingName);
            Log.d("item.settingName", "item.settingName (" + item.settingName + "): "+item.value.toString().length());
            String textValue = item.value != null ? item.value.toString() : "";
            holder.textInputLayout.setHintAnimationEnabled(false);

            insertTextChunked(holder.textInputEditText, textValue);

        } else if ("int".equalsIgnoreCase(item.settingType) || "float".equalsIgnoreCase(item.settingType) || "long".equalsIgnoreCase(item.settingType)) {
            holder.stringLayout.setVisibility(View.GONE);
            holder.intLayout.setVisibility(View.VISIBLE);
            holder.IntOutlinedTextField.setHint(item.settingName);
            String textValue = "";
            if (item.value != null) {
                textValue = item.value.toString();
            }
            holder.textInputEditInt.setText(textValue);
        }

        // Для других типов можно расширить логику
    }

    @Override
    public int getItemCount() {
        return settings.size();
    }

    static class SettingsViewHolder extends RecyclerView.ViewHolder {
        TextView VarType;
        ConstraintLayout booleanLayout;
        MaterialSwitch materialSwitch;

        ConstraintLayout stringLayout, intLayout;
        TextInputLayout textInputLayout, IntOutlinedTextField;
        TextInputEditText textInputEditText, textInputEditInt;

        @SuppressLint("CutPasteId")
        public SettingsViewHolder(@NonNull View itemView) {
            super(itemView);
            booleanLayout = itemView.findViewById(R.id.BooleanType);
            materialSwitch = itemView.findViewById(R.id.materialSwitch);

            stringLayout = itemView.findViewById(R.id.StringType); intLayout = itemView.findViewById(R.id.IntType);
            textInputLayout = itemView.findViewById(R.id.outlinedTextField);
            textInputEditText = itemView.findViewById(R.id.settings_edit_text);
            IntOutlinedTextField = itemView.findViewById(R.id.IntOutlinedTextField);
            textInputEditInt = itemView.findViewById(R.id.settings_edit_int);
            VarType = itemView.findViewById(R.id.VarType);
        }
    }
}


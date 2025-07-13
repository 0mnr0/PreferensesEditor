package com.dsvl0.preferenseseditor;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
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
    boolean isEditTextLoading = false;

    public SettingsAdapter(List<SettingItem> settings) {
        this.settings = settings;
    }

    @NonNull
    @Override
    public SettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.setting_edit_layout, parent, false);
        return new SettingsViewHolder(view);
    }

    private void insertTextChunked(SettingsViewHolder holder, String fullText) {
        if (TextUtils.isEmpty(fullText)) {
            holder.textInputEditText.setText("");
            return;
        }

        final int length = fullText.length();
        holder.isTextLoading = true;
        holder.textInputEditText.setEnabled(false);

        // Оптимизация для EditText
        optimizeEditTextForLargeContent(holder.textInputEditText);

        // Определение размера чанка в зависимости от длины
        int chunkSize = calculateChunkSize(length);
        final int delay = calculateDelay(length);

        holder.textInputEditText.setText(""); // Начинаем с пустого поля

        holder.insertRunnable = new Runnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= length || holder.isCancelled) {
                    holder.isTextLoading = false;
                    holder.textInputEditText.setEnabled(true);
                    return;
                }

                int end = Math.min(index + chunkSize, length);
                holder.textInputEditText.append(fullText.substring(index, end));
                index = end;

                if (index < length) {
                    holder.textInputEditText.postDelayed(this, delay);
                } else {
                    holder.isTextLoading = false;
                    holder.textInputEditText.setEnabled(true);
                }
            }
        };

        holder.textInputEditText.post(holder.insertRunnable);
    }


    private int calculateChunkSize(int length) {
        if (length <= 10_000) return length; // Вставляем сразу
        if (length <= 50_000) return 5_000;
        if (length <= 200_000) return 10_000;
        if (length <= 500_000) return 20_000;
        return 30_000; // Для текстов > 500k символов
    }

    private int calculateDelay(int length) {
        if (length <= 100_000) return 0;   // Без задержки для средних текстов
        if (length <= 500_000) return 20;  // 20ms
        return 50;                          // 50ms для очень больших текстов
    }

    @Override
    public void onViewRecycled(@NonNull SettingsViewHolder holder) {
        super.onViewRecycled(holder);
        holder.cancelPendingInsert(); // Отмена операций при переиспользовании ViewHolder
    }


    @Override
    public void onBindViewHolder(@NonNull SettingsViewHolder holder, int position) {
        SettingItem item = settings.get(position);
        holder.resetState();
        holder.VarType.setText(item.settingType);
        Object originalValue; originalValue = item.value;



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
            holder.materialSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                holder.ChangesLayout.setVisibility((isChecked != (Boolean) item.value) ? View.VISIBLE : View.GONE);
            });

        } else if ("string".equalsIgnoreCase(item.settingType)) {
            holder.stringLayout.setVisibility(View.VISIBLE);
            holder.textInputLayout.setHint(item.settingName);
            Log.d("item.settingName", "item.settingName (" + item.settingName + "): "+item.value.toString().length());
            String textValue = item.value != null ? item.value.toString() : "";
            holder.textInputLayout.setHintAnimationEnabled(false);
            insertTextChunked(holder, textValue);
            optimizeEditTextForLargeContent(holder.textInputEditText);
            holder.textInputEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isEditTextLoading) { return; }
                    String newValue = s.toString();
                    holder.ChangesLayout.setVisibility((!newValue.equals(originalValue)) ? View.VISIBLE : View.GONE);
                }
            });



        } else if ("int".equalsIgnoreCase(item.settingType) || "float".equalsIgnoreCase(item.settingType) || "long".equalsIgnoreCase(item.settingType)) {
            holder.stringLayout.setVisibility(View.GONE);
            holder.intLayout.setVisibility(View.VISIBLE);
            holder.IntOutlinedTextField.setHint(item.settingName);
            String textValue = "";
            if (item.value != null) {
                textValue = item.value.toString();
            }
            holder.textInputEditInt.setText(textValue);
            holder.textInputEditInt.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isEditTextLoading) { return; }
                    boolean isChangedFromOriginal = !s.toString().equals(originalValue.toString());
                    String Type = item.settingType;
                    switch (item.settingType) {
                        case "int":
                            item.value = Integer.parseInt(s.toString());
                            break;
                        case "float":
                            item.value = Float.parseFloat(s.toString());
                            break;
                        case "long":
                            item.value = Long.parseLong(s.toString());
                            break;
                    }
                    holder.ChangesLayout.setVisibility(isChangedFromOriginal ? View.VISIBLE : View.GONE);
                }
            });
        }

        // Для других типов можно расширить логику
    }

    @Override
    public int getItemCount() {
        return settings.size();
    }
    private void optimizeEditTextForLargeContent(EditText editText) {
        editText.setInputType(editText.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setHorizontallyScrolling(true);
        editText.setMovementMethod(null);
        editText.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        editText.setScrollContainer(true);
    }

    static class SettingsViewHolder extends RecyclerView.ViewHolder {
        boolean isTextLoading = false;
        boolean isCancelled = false;
        Runnable insertRunnable;

        public void resetState() {
            isTextLoading = false;
            isCancelled = false;
        }
        public void cancelPendingInsert() {
            isCancelled = true;
            if (insertRunnable != null && textInputEditText != null) {
                textInputEditText.removeCallbacks(insertRunnable);
            }
        }


        TextView VarType;
        ConstraintLayout booleanLayout;
        MaterialSwitch materialSwitch;
        ConstraintLayout stringLayout, intLayout, ChangesLayout;
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
            ChangesLayout = itemView.findViewById(R.id.ApplyOrDeny);
            VarType = itemView.findViewById(R.id.VarType);
        }
    }
}


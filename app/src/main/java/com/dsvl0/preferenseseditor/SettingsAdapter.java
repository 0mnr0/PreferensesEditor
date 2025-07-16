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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder> {

    private List<SettingItem> settings;
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
                    holder.textInputEditText.clearFocus();
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



        holder.booleanLayout.setVisibility(View.GONE);
        holder.stringLayout.setVisibility(View.GONE);
        holder.setLayout.setVisibility(View.GONE);
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
                item.value = isChecked;
            });

        } else if ("string".equalsIgnoreCase(item.settingType)) {
            holder.stringLayout.setVisibility(View.VISIBLE);
            holder.textInputLayout.setHint(item.settingName);
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
                    item.value = s.toString();
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
                }
            });
        } else if ("set".equalsIgnoreCase(item.settingType)) {
            holder.setLayout.setVisibility(View.VISIBLE);
            final HashSet finalValue = (HashSet) item.value;
            LayoutInflater inflater = LayoutInflater.from(holder.setList.getContext());
            holder.CreateNewSetElement.setOnClickListener(v -> {
                AddSetToList(inflater, holder, "", item);
            });


            for (int i = 0; i < finalValue.size(); i++) {
                AddSetToList(inflater, holder, finalValue.toArray()[i].toString(), item);
            }
        }
    }

    @SuppressLint("LongLogTag")
    private void AddSetToList(LayoutInflater inflater, SettingsViewHolder holder, String finalValue, SettingItem item) {
        View SetManipulator = inflater.inflate(R.layout.set_manipulator, holder.setList, false);

        EditText editText = SetManipulator.findViewById(R.id.editText);
        ImageView DeleteAction = SetManipulator.findViewById(R.id.deleteAction);
        editText.setHint((finalValue.isEmpty()) ? "Новое значение: " : finalValue);
        editText.setText((finalValue.isEmpty()) ? "" : finalValue);

        final int CurrentElement = holder.SetList.size();
        holder.SetList.add(CurrentElement, editText.getText().toString());
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @SuppressLint("LongLogTag")
            @Override
            public void afterTextChanged(Editable s) {
                Log.d("CurrentElement (Editing): ", CurrentElement + " | " + s.toString());
                holder.SetList.set(CurrentElement, s.toString());
                item.value = holder.SetList;
            }
        });

        DeleteAction.setOnClickListener(v -> {
            final int CurrentReindexedElement = holder.setList.indexOfChild(SetManipulator) - 1;
            holder.setList.removeView(SetManipulator);
            holder.SetList.remove(CurrentReindexedElement);
        });

        holder.setList.addView(SetManipulator);
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
        ConstraintLayout stringLayout, intLayout, setLayout;
        TextInputLayout textInputLayout, IntOutlinedTextField;
        TextInputEditText textInputEditText, textInputEditInt;
        LinearLayout setList;
        Button CreateNewSetElement;
        ArrayList<String> SetList = new ArrayList<>();

        @SuppressLint("CutPasteId")
        public SettingsViewHolder(@NonNull View itemView) {
            super(itemView);
            booleanLayout = itemView.findViewById(R.id.BooleanType);
            materialSwitch = itemView.findViewById(R.id.materialSwitch);
            setList = itemView.findViewById(R.id.setList);
            stringLayout = itemView.findViewById(R.id.StringType); intLayout = itemView.findViewById(R.id.IntType);
            textInputLayout = itemView.findViewById(R.id.outlinedTextField);
            textInputEditText = itemView.findViewById(R.id.settings_edit_text);
            IntOutlinedTextField = itemView.findViewById(R.id.IntOutlinedTextField);
            textInputEditInt = itemView.findViewById(R.id.settings_edit_int);
            setLayout = itemView.findViewById(R.id.SetType);
            VarType = itemView.findViewById(R.id.VarType);
            CreateNewSetElement = itemView.findViewById(R.id.CreateNewSetElement);
        }
    }


    public void CreateNewSetting(String SettingType, String SettingName, Object SettingValue) {
        settings.add(new SettingItem(SettingName, SettingType, SettingValue));
        notifyDataSetChanged();
    }

    public List<SettingItem> ExportData() {
        return settings;
    }
}


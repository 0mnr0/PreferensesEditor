package com.dsvl0.preferenseseditor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashSet;
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
        holder.textInputEditText.setMovementMethod(new ScrollingMovementMethod());
        holder.textInputEditText.setHorizontallyScrolling(true);

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


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull SettingsViewHolder holder, int position) {
        SettingItem item = settings.get(position);
        holder.resetState();
        holder.VarType.setText(item.settingType);
        holder.MainLayout.setOnLongClickListener(v -> {
            showDeleteDialog(holder, item, holder.MainLayout.getContext(), item.settingType);
            return true;
        });


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
            holder.materialSwitch.setOnTouchListener((v, e) -> {holder.MainLayout.onTouchEvent(e); return false;});
            holder.materialSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> item.value = isChecked);

        } else if ("string".equalsIgnoreCase(item.settingType)) {
            holder.stringLayout.setVisibility(View.VISIBLE);
            holder.textInputLayout.setHint(item.settingName);
            String textValue = item.value != null ? item.value.toString() : "";
            holder.textInputLayout.setHintAnimationEnabled(false);
            insertTextChunked(holder, textValue);
            optimizeEditTextForLargeContent(holder.textInputEditText);
            holder.textInputEditText.setMovementMethod(new ScrollingMovementMethod());
            holder.textInputEditText.setHorizontallyScrolling(true);
            holder.textInputEditText.setOnTouchListener((v, e) -> {holder.MainLayout.onTouchEvent(e); return false;});
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
            holder.textInputEditInt.setMovementMethod(new ScrollingMovementMethod());
            holder.textInputEditInt.setHorizontallyScrolling(true);
            holder.textInputEditInt.setOnTouchListener((v, e) -> {holder.MainLayout.onTouchEvent(e); return false;});
            holder.textInputEditInt.addTextChangedListener(new TextWatcher() {
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
        } else if ("set".equalsIgnoreCase(item.settingType)) {
            holder.setLayout.setVisibility(View.VISIBLE);
            final HashSet finalValue = (HashSet) item.value;
            LayoutInflater inflater = LayoutInflater.from(holder.setList.getContext());
            holder.PreferenceName.setText(item.settingName);
            holder.CreateNewSetElement.setOnTouchListener((v, e) -> {holder.MainLayout.onTouchEvent(e); return false;});
            holder.CreateNewSetElement.setOnClickListener(v -> {
                AddSetToList(inflater, holder, "", item);
            });

            for (int i = 0; i < finalValue.size(); i++) {
                AddSetToList(inflater, holder, finalValue.toArray()[i].toString(), item);
            }
        }
    }

    private void showDeleteDialog(SettingsViewHolder holder, SettingItem item, Context context, String type) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.setting_action_dialog, null);
        Spinner spinner = dialogView.findViewById(R.id.changeVarType);
        Button RemoveKey = dialogView.findViewById(R.id.RemoveKey);


        String[] options = {"String", "Int", "Float", "Long"};
        if (type.equalsIgnoreCase("set") || type.equalsIgnoreCase("boolean")){
            options = new String[]{context.getString(R.string.NotAvaiableFor__TYPE__) + " <" + type + ">"};
            spinner.setEnabled(false);
            spinner.setClickable(false);
        }

        int index = 0;
        for (int i = 0; i < options.length; i++) { if (options[i].equalsIgnoreCase(type)){ index = i; } }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, options);
        spinner.setAdapter(adapter);
        spinner.setSelection(index);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setView(dialogView)
                .setTitle("Выберите тип переменной")
                .setCancelable(true)
                .setPositiveButton("Далее", (dialog, which) -> {
                    String selected = spinner.getSelectedItem().toString().toLowerCase();
                    item.settingType = selected;
                    holder.VarType.setText(selected);
                })
                .setNegativeButton("Отмена", (dialog, which) -> {});

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        RemoveKey.setOnClickListener(v -> {
            item.value = null;
            item.settingType = null;
            item.settingName = null;
            holder.MainLayout.removeAllViews();
            dialog.dismiss();
        });

        dialog.show();
    }

    @SuppressLint({"LongLogTag", "ClickableViewAccessibility"})
    private void AddSetToList(LayoutInflater inflater, SettingsViewHolder holder, String finalValue, SettingItem item) {
        View SetManipulator = inflater.inflate(R.layout.set_manipulator, holder.setList, false);

        EditText editText = SetManipulator.findViewById(R.id.editText);
        ImageView DeleteAction = SetManipulator.findViewById(R.id.deleteAction);
        editText.setHint((finalValue.isEmpty()) ? "Новое значение: " : finalValue);
        editText.setText((finalValue.isEmpty()) ? "" : finalValue);

        final int CurrentElement = holder.SetList.size();

        holder.SetList.add(CurrentElement, editText.getText().toString());

        editText.setOnTouchListener((v, e) -> {holder.MainLayout.onTouchEvent(e); return false;});
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @SuppressLint("LongLogTag")
            @Override
            public void afterTextChanged(Editable s) {
                holder.SetList.set(CurrentElement, s.toString());
                item.value = holder.SetList;
                settings.set(settings.indexOf(item), item);
            }
        });

        DeleteAction.setOnTouchListener((v, e) -> {holder.MainLayout.onTouchEvent(e); return false;});
        DeleteAction.setOnClickListener(v -> {
            final int CurrentReindexedElement = holder.setList.indexOfChild(SetManipulator) - 2; // -2 Потому что в layout есть дополнительный TextView и Button

            holder.setList.removeView(SetManipulator);
            holder.SetList.remove(CurrentReindexedElement);
            item.value = holder.SetList;
            settings.set(settings.indexOf(item), item);
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


        TextView VarType, PreferenceName;
        ConstraintLayout booleanLayout;
        MaterialSwitch materialSwitch;
        ConstraintLayout stringLayout, intLayout, setLayout;
        TextInputLayout textInputLayout, IntOutlinedTextField;
        TextInputEditText textInputEditText, textInputEditInt;
        LinearLayout setList;
        MaterialButton CreateNewSetElement;
        ArrayList<String> SetList = new ArrayList<>();
        ConstraintLayout MainLayout;

        @SuppressLint("CutPasteId")
        public SettingsViewHolder(@NonNull View itemView) {
            super(itemView);
            MainLayout = (ConstraintLayout) itemView;
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
            PreferenceName = itemView.findViewById(R.id.PreferenceName);
        }
    }


    public void AddSetting(String SettingName, String SettingType, Object SettingValue) {
        settings.add(new SettingItem(SettingName, SettingType, SettingValue));
        notifyItemInserted(settings.size() - 1);
    }

    public List<SettingItem> ExportData() {
        return settings;
    }
}


package com.example.onfire_project.dashboard.history;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.onfire_project.R;
import com.example.onfire_project.databinding.FragmentHistoryBinding;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryAdapter adapter;

    private final ArrayList<HistoryItem> allItems = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // exemplo JSON (substituir pela tua origem real)
        String jsonString = "[\n" +
                "  {\n" +
                "    \"type\": \"walking\",\n" +
                "    \"pace\": \"9:30\",\n" +
                "    \"speed\": 6.33,\n" +
                "    \"distance\": 0.11,\n" +
                "    \"calories\": 5,\n" +
                "    \"avg_speed\": 6.33,\n" +
                "    \"duration\": \"00:01:00\",\n" +
                "    \"steps\": 137,\n" +
                "    \"date\": \"2025-11-05\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"running\",\n" +
                "    \"pace\": \"5:10\",\n" +
                "    \"speed\": 11.6,\n" +
                "    \"distance\": 3.2,\n" +
                "    \"calories\": 220,\n" +
                "    \"avg_speed\": 11.4,\n" +
                "    \"duration\": \"00:16:45\",\n" +
                "    \"steps\": 4210,\n" +
                "    \"date\": \"2025-11-04\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"cycling\",\n" +
                "    \"pace\": \"2:45\",\n" +
                "    \"speed\": 21.8,\n" +
                "    \"distance\": 15.3,\n" +
                "    \"calories\": 480,\n" +
                "    \"avg_speed\": 21.5,\n" +
                "    \"duration\": \"00:42:00\",\n" +
                "    \"steps\": 0,\n" +
                "    \"date\": \"2025-11-03\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"walking\",\n" +
                "    \"pace\": \"10:15\",\n" +
                "    \"speed\": 5.85,\n" +
                "    \"distance\": 2.3,\n" +
                "    \"calories\": 120,\n" +
                "    \"avg_speed\": 5.9,\n" +
                "    \"duration\": \"00:23:35\",\n" +
                "    \"steps\": 2980,\n" +
                "    \"date\": \"2025-11-02\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"running\",\n" +
                "    \"pace\": \"4:45\",\n" +
                "    \"speed\": 12.6,\n" +
                "    \"distance\": 5.0,\n" +
                "    \"calories\": 380,\n" +
                "    \"avg_speed\": 12.4,\n" +
                "    \"duration\": \"00:23:45\",\n" +
                "    \"steps\": 6550,\n" +
                "    \"date\": \"2025-11-01\"\n" +
                "  }\n" +
                "]";

        // popula allItems (funciona com objeto ou array)
        parseJsonToList(jsonString);

        // inicializa adapter com cópia da lista completa
        adapter = new HistoryAdapter(requireContext(), new ArrayList<>(allItems));
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);

        // botões de filtro
        final List<MaterialButton> buttons = Arrays.asList(
                binding.fltAll,
                binding.fltRunning,
                binding.fltBycicle,
                binding.fltWalking
        );

        // estilo inicial: "All" selecionado
        updateButtonStyles(buttons, binding.fltAll);

        // listeners
        for (MaterialButton btn : buttons) {
            btn.setOnClickListener(v -> {
                updateButtonStyles(buttons, btn);
                filterListByButton(btn);
            });
        }

        return root;
    }

    // atualiza estilos dos botões: selected -> primary + bold, others -> background + normal
    private void updateButtonStyles(List<MaterialButton> buttons, MaterialButton selected) {
        int colorPrimary = ContextCompat.getColor(requireContext(), R.color.primary);
        int colorBackground = ContextCompat.getColor(requireContext(), R.color.background);
        int colorSelectedText = ContextCompat.getColor(requireContext(), R.color.foreground); // texto do botão selecionado
        int colorUnselectedText = colorSelectedText; // texto dos não selecionados (ajusta se quiser outro)

        for (MaterialButton btn : buttons) {
            if (btn == selected) {
                btn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                btn.setTypeface(null, Typeface.BOLD);
                btn.setTextColor(colorSelectedText);
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(colorBackground));
                btn.setTypeface(null, Typeface.NORMAL);
                btn.setTextColor(colorUnselectedText);
            }
        }
    }

    // faz a filtragem conforme botão e atualiza adapter
    private void filterListByButton(MaterialButton btn) {
        int id = btn.getId();
        ArrayList<HistoryItem> filtered = new ArrayList<>();

        if (id == R.id.fltRunning) {
            for (HistoryItem it : allItems) {
                if (it.type != null && it.type.equalsIgnoreCase("running")) filtered.add(it);
            }
        } else if (id == R.id.fltBycicle) {
            for (HistoryItem it : allItems) {
                if (it.type != null) {
                    String t = it.type.toLowerCase();
                    if (t.equals("cycling") || t.equals("bike") || t.equals("biking")) filtered.add(it);
                }
            }
        } else if (id == R.id.fltWalking) {
            for (HistoryItem it : allItems) {
                if (it.type != null && it.type.equalsIgnoreCase("walking")) filtered.add(it);
            }
        } else { // fltAll
            filtered.addAll(allItems);
        }

        adapter.updateList(filtered);
    }

    // --- JSON parsing (mesma função tua, adaptada para preencher allItems) ---
    private void parseJsonToList(String jsonString) {
        allItems.clear();
        try {
            jsonString = jsonString.trim();
            if (jsonString.startsWith("[")) {
                JSONArray arr = new JSONArray(jsonString);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    allItems.add(jsonObjectToHistoryItem(o));
                }
            } else if (jsonString.startsWith("{")) {
                JSONObject o = new JSONObject(jsonString);
                allItems.add(jsonObjectToHistoryItem(o));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("HistoryFragment", "Failed to parse JSON: " + e.getMessage());
        }
    }

    private HistoryItem jsonObjectToHistoryItem(JSONObject o) {
        try {
            String type = o.optString("type", "");
            String pace = o.optString("pace", null);
            double speed = 0;
            try {
                String s = o.optString("speed", "0");
                speed = Double.parseDouble(s);
            } catch (Exception ignored) {}
            double distance = o.optDouble("distance", 0);
            int calories = o.optInt("calories", 0);
            double avg_speed = o.optDouble("avg_speed", 0);
            String duration = o.optString("duration", "");
            int steps = o.optInt("steps", 0);
            String date = o.optString("date", "");

            return new HistoryItem(type, pace, speed, distance, calories, avg_speed, duration, steps, date);
        } catch (Exception e) {
            e.printStackTrace();
            return new HistoryItem("", null, 0,0,0,0,"",0,"");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

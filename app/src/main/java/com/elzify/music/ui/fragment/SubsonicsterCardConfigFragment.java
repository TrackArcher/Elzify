package com.elzify.music.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elzify.music.R;
import com.elzify.music.subsonicster.SubsonicsterCsvRepository;
import com.elzify.music.ui.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubsonicsterCardConfigFragment extends Fragment {

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private SubsonicsterCsvRepository csvRepository;
    private ArrayAdapter<String> filesAdapter;
    private final List<String> files = new ArrayList<>();
    private TextView statusText;

    private ActivityResultLauncher<Intent> csvPickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        View view = inflater.inflate(R.layout.fragment_subsonicster_card_config, container, false);
        csvRepository = new SubsonicsterCsvRepository(requireContext());

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> activity.navController.navigateUp());
        }

        statusText = view.findViewById(R.id.subsonicster_card_config_status);
        Button addLocalButton = view.findViewById(R.id.subsonicster_add_local_csv_button);
        ListView listView = view.findViewById(R.id.subsonicster_csv_list);

        filesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, files);
        listView.setAdapter(filesAdapter);
        listView.setOnItemLongClickListener((parent, itemView, position, id) -> {
            String name = files.get(position);
            ioExecutor.execute(() -> {
                csvRepository.deleteCsv(name);
                requireActivity().runOnUiThread(() -> {
                    setStatus("Deleted: " + name);
                    refreshFileList();
                });
            });
            return true;
        });

        csvPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK) return;
                    Intent data = result.getData();
                    if (data == null || data.getData() == null) return;
                    Uri uri = data.getData();
                    setStatus("Importing local CSV...");
                    ioExecutor.execute(() -> {
                        try {
                            String saved = csvRepository.saveLocalCsv(uri, null);
                            requireActivity().runOnUiThread(() -> {
                                setStatus("Imported: " + saved);
                                refreshFileList();
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() -> setStatus("Import failed: " + e.getMessage()));
                        }
                    });
                }
        );

        addLocalButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            csvPickerLauncher.launch(intent);
        });

        refreshFileList();
        return view;
    }

    private void refreshFileList() {
        files.clear();
        files.addAll(csvRepository.getAllLocalCsvFiles());
        filesAdapter.notifyDataSetChanged();
    }

    private void setStatus(String message) {
        statusText.setText(message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ioExecutor.shutdownNow();
    }
}

package com.elzify.music.ui.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import com.elzify.music.App;
import com.elzify.music.R;
import com.elzify.music.subsonic.base.ApiResponse;
import com.elzify.music.subsonic.models.Child;
import com.elzify.music.subsonicster.SubsonicsterCsvRepository;
import com.elzify.music.ui.activity.MainActivity;
import com.elzify.music.util.MusicUtil;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

@OptIn(markerClass = UnstableApi.class)
public class SubsonicsterScanQrFragment extends Fragment {

    private static final String TAG = "SubsonicsterScanQr";

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private SubsonicsterCsvRepository csvRepository;
    private DecoratedBarcodeView scannerView;
    private View scannerOverlayView;
    private View scannerCenterIcon;
    private View controlsContainer;
    private Button permissionButton;
    private MaterialButton playPauseButton;
    private volatile boolean isProcessingScan = false;
    private boolean hasPlayableTrack = false;
    private ExoPlayer gamePlayer;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        View view = inflater.inflate(R.layout.fragment_subsonicster_scan_qr, container, false);
        csvRepository = new SubsonicsterCsvRepository(requireContext());

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> activity.navController.navigateUp());
        }

        scannerView = view.findViewById(R.id.subsonicster_qr_scanner_view);
        scannerOverlayView = view.findViewById(R.id.subsonicster_scanner_overlay);
        scannerCenterIcon = view.findViewById(R.id.subsonicster_qr_center_icon);
        controlsContainer = view.findViewById(R.id.subsonicster_controls_container);
        playPauseButton = view.findViewById(R.id.subsonicster_play_pause_button);
        Button scanNextButton = view.findViewById(R.id.subsonicster_scan_next_button);
        permissionButton = view.findViewById(R.id.subsonicster_scan_permission_button);

        scannerView.getStatusView().setVisibility(View.GONE);
        hideDefaultViewfinderOverlay();
        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.QR_CODE);
        formats.add(BarcodeFormat.AZTEC);
        formats.add(BarcodeFormat.DATA_MATRIX);
        // Use mixed decode mode (normal + inverted), matching Subsonicster behavior.
        scannerView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats, null, null, 2));

        scannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result == null || result.getText() == null) return;
                if (isProcessingScan) return;
                isProcessingScan = true;
                scannerView.pause();
                updateUiForScannerState(false);
                onQrScanned(result.getText());
            }
        });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        permissionButton.setVisibility(View.GONE);
                        scannerView.setVisibility(View.VISIBLE);
                        scannerView.resume();
                    } else {
                        permissionButton.setVisibility(View.VISIBLE);
                        scannerView.pause();
                        scannerView.setVisibility(View.INVISIBLE);
                    }
                }
        );

        scanNextButton.setOnClickListener(v -> resetScanner());
        playPauseButton.setOnClickListener(v -> togglePlayback());
        playPauseButton.setEnabled(false);
        playPauseButton.setText("");
        playPauseButton.setIconResource(R.drawable.ic_play);
        playPauseButton.setContentDescription(getString(R.string.subsonicster_play));
        permissionButton.setOnClickListener(v -> requestCameraPermission());

        ensureCameraPermission();
        return view;
    }

    private void onQrScanned(String qrText) {
        ioExecutor.execute(() -> {
            try {
                String supported = SubsonicsterCsvRepository.parseSupportedQrUrl(qrText);
                if (supported == null) {
                    return;
                }

                SubsonicsterCsvRepository.CsvSong mapped = csvRepository.findSongForQrContent(supported);
                if (mapped == null) {
                    return;
                }

                Child bestMatch = searchBestSong(mapped.title, mapped.artist);
                if (bestMatch == null) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "No playing song found on Subsonic", Toast.LENGTH_LONG).show());
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    playTrack(bestMatch);
                });
            } catch (Exception e) {
                Log.e(TAG, "QR processing failed", e);
            }
        });
    }

    @Nullable
    private Child searchBestSong(String title, String artist) {
        try {
            String query = (title + " " + artist).trim();
            Response<ApiResponse> response = App.getSubsonicClientInstance(false)
                    .getSearchingClient()
                    .search3(query, 100, 0, 0, 0, 0, 0)
                    .execute();
            if (!response.isSuccessful() || response.body() == null
                    || response.body().getSubsonicResponse().getSearchResult3() == null
                    || response.body().getSubsonicResponse().getSearchResult3().getSongs() == null) {
                return null;
            }
            List<Child> songs = response.body().getSubsonicResponse().getSearchResult3().getSongs();
            if (songs.isEmpty()) return null;

            String expectedTitle = norm(title);
            String expectedArtist = norm(artist);

            Child containsTitleAndArtist = null;
            for (Child song : songs) {
                String songTitle = norm(song.getTitle());
                String songArtist = norm(song.getArtist());
                boolean titleEqual = songTitle.equals(expectedTitle);
                boolean artistEqual = songArtist.equals(expectedArtist);
                if (titleEqual && artistEqual) {
                    return song;
                }
                boolean titleContains = !expectedTitle.isEmpty() && songTitle.contains(expectedTitle);
                boolean artistContains = !expectedArtist.isEmpty() && songArtist.contains(expectedArtist);
                if (titleContains && artistContains && containsTitleAndArtist == null) {
                    containsTitleAndArtist = song;
                }
            }
            return containsTitleAndArtist != null ? containsTitleAndArtist : songs.get(0);
        } catch (Exception e) {
            Log.e(TAG, "Subsonic search failed", e);
            return null;
        }
    }

    private void resetScanner() {
        stopPlayback();
        isProcessingScan = false;
        hasPlayableTrack = false;
        playPauseButton.setEnabled(false);
        playPauseButton.setText("");
        playPauseButton.setIconResource(R.drawable.ic_play);
        playPauseButton.setContentDescription(getString(R.string.subsonicster_play));
        if (hasCameraPermission()) {
            updateUiForScannerState(true);
            scannerView.resume();
        }
    }

    private void ensureCameraPermission() {
        if (hasCameraPermission()) {
            permissionButton.setVisibility(View.GONE);
            updateUiForScannerState(!isProcessingScan);
            scannerView.resume();
        } else {
            permissionButton.setVisibility(View.VISIBLE);
            scannerView.pause();
            updateUiForScannerState(false);
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static String norm(@Nullable String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.US);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.setBottomNavigationBarVisibility(false);
            activity.setBottomSheetVisibility(false);
        }
        if (scannerView != null && hasCameraPermission() && !isProcessingScan) {
            updateUiForScannerState(true);
            scannerView.resume();
        }
    }

    @Override
    public void onPause() {
        if (scannerView != null) {
            scannerView.pause();
        }
        stopPlayback();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.setBottomNavigationBarVisibility(true);
            activity.setBottomSheetVisibility(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releasePlayer();
        ioExecutor.shutdownNow();
    }

    private void ensurePlayer() {
        if (gamePlayer == null) {
            gamePlayer = new ExoPlayer.Builder(requireContext()).build();
        }
    }

    private void playTrack(Child child) {
        if (child == null) return;
        ensurePlayer();
        MediaItem item = new MediaItem.Builder()
                .setMediaId(child.getId())
                .setUri(MusicUtil.getStreamUri(child.getId()))
                .build();
        gamePlayer.setMediaItem(item);
        gamePlayer.prepare();
        gamePlayer.play();
        hasPlayableTrack = true;
        playPauseButton.setEnabled(true);
        playPauseButton.setText("");
        playPauseButton.setIconResource(R.drawable.ic_pause);
        playPauseButton.setContentDescription(getString(R.string.subsonicster_pause));
    }

    private void togglePlayback() {
        if (!hasPlayableTrack) return;
        ensurePlayer();
        if (gamePlayer.isPlaying()) {
            gamePlayer.pause();
            playPauseButton.setText("");
            playPauseButton.setIconResource(R.drawable.ic_play);
            playPauseButton.setContentDescription(getString(R.string.subsonicster_play));
        } else {
            gamePlayer.play();
            playPauseButton.setText("");
            playPauseButton.setIconResource(R.drawable.ic_pause);
            playPauseButton.setContentDescription(getString(R.string.subsonicster_pause));
        }
    }

    private void stopPlayback() {
        if (gamePlayer != null) {
            gamePlayer.stop();
        }
        if (playPauseButton != null) {
            playPauseButton.setText("");
            playPauseButton.setIconResource(R.drawable.ic_play);
            playPauseButton.setContentDescription(getString(R.string.subsonicster_play));
        }
    }

    private void releasePlayer() {
        if (gamePlayer != null) {
            gamePlayer.release();
            gamePlayer = null;
        }
    }

    private void updateUiForScannerState(boolean scannerActive) {
        if (scannerView != null) {
            scannerView.setVisibility(scannerActive ? View.VISIBLE : View.INVISIBLE);
        }
        if (scannerOverlayView != null) {
            scannerOverlayView.setVisibility(scannerActive ? View.VISIBLE : View.INVISIBLE);
        }
        if (scannerCenterIcon != null) {
            scannerCenterIcon.setVisibility(scannerActive ? View.VISIBLE : View.INVISIBLE);
        }
        if (controlsContainer != null) {
            controlsContainer.setVisibility(scannerActive ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void hideDefaultViewfinderOverlay() {
        if (scannerView == null) {
            return;
        }

        int viewfinderId = scannerView.getResources().getIdentifier(
                "zxing_viewfinder_view",
                "id",
                requireContext().getPackageName()
        );

        if (viewfinderId == 0) {
            viewfinderId = scannerView.getResources().getIdentifier(
                    "zxing_viewfinder_view",
                    "id",
                    "com.google.zxing.client.android"
            );
        }

        if (viewfinderId != 0) {
            View defaultViewfinder = scannerView.findViewById(viewfinderId);
            if (defaultViewfinder != null) {
                defaultViewfinder.setVisibility(View.GONE);
            }
        }
    }
}

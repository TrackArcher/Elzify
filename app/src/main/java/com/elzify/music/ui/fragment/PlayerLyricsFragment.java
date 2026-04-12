package com.elzify.music.ui.fragment;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;

import com.elzify.music.R;
import com.elzify.music.databinding.InnerFragmentPlayerLyricsBinding;
import com.elzify.music.service.MediaService;
import com.elzify.music.subsonic.models.Line;
import com.elzify.music.subsonic.models.LyricsList;
import com.elzify.music.util.MusicUtil;
import com.elzify.music.util.Preferences;
import com.elzify.music.util.UIUtil;
import com.elzify.music.viewmodel.PlayerBottomSheetViewModel;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;


@OptIn(markerClass = UnstableApi.class)
public class PlayerLyricsFragment extends Fragment {
    private static final String TAG = "PlayerLyricsFragment";

    private InnerFragmentPlayerLyricsBinding bind;
    private PlayerBottomSheetViewModel playerBottomSheetViewModel;
    private ListenableFuture<MediaBrowser> mediaBrowserListenableFuture;
    private MediaBrowser mediaBrowser;
    private Handler syncLyricsHandler;
    private Runnable syncLyricsRunnable;
    private String currentLyrics;
    private LyricsList currentLyricsList;
    private Integer lastLineIdx;
    private String currentDescription;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        bind = InnerFragmentPlayerLyricsBinding.inflate(inflater, container, false);
        View view = bind.getRoot();

        playerBottomSheetViewModel = new ViewModelProvider(requireActivity()).get(PlayerBottomSheetViewModel.class);

        applyPlayerBackgroundColor();

        return view;
    }

    private void applyPlayerBackgroundColor() {
        if (bind == null) return;
        int playerBackgroundColor = UIUtil.getPlayerBackgroundColor(requireContext());
        bind.getRoot().setBackgroundColor(playerBackgroundColor);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initPanelContent();
    }

    @Override
    public void onStart() {
        super.onStart();
        initializeBrowser();
        applyPlayerBackgroundColor();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindMediaController();
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseHandler();
        if (!Preferences.isDisplayAlwaysOn()) {
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onStop() {
        releaseBrowser();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
        currentLyrics = null;
        currentLyricsList = null;
        currentDescription = null;
        lastLineIdx = null;
    }

    private void initializeBrowser() {
        mediaBrowserListenableFuture = new MediaBrowser.Builder(requireContext(), new SessionToken(requireContext(), new ComponentName(requireContext(), MediaService.class))).buildAsync();
    }

    private void releaseHandler() {
        if (syncLyricsHandler != null) {
            syncLyricsHandler.removeCallbacks(syncLyricsRunnable);
            syncLyricsHandler = null;
        }
    }

    private void releaseBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture);
    }

    private void bindMediaController() {
        mediaBrowserListenableFuture.addListener(() -> {
            try {
                mediaBrowser = mediaBrowserListenableFuture.get();
                defineProgressHandler();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void initPanelContent() {
        playerBottomSheetViewModel.getLiveLyrics().observe(getViewLifecycleOwner(), lyrics -> {
            currentLyrics = lyrics;
            updatePanelContent();
        });

        playerBottomSheetViewModel.getLiveLyricsList().observe(getViewLifecycleOwner(), lyricsList -> {
            currentLyricsList = lyricsList;
            lastLineIdx = null;
            updatePanelContent();
        });

        playerBottomSheetViewModel.getLiveDescription().observe(getViewLifecycleOwner(), description -> {
            currentDescription = description;
            updatePanelContent();
        });
    }

    private void updatePanelContent() {
        if (bind == null) {
            return;
        }

        bind.nowPlayingSongLyricsSrollView.smoothScrollTo(0, 0);

        if (hasStructuredLyrics(currentLyricsList)) {
            setSyncLyrics(currentLyricsList);
            bind.nowPlayingSongLyricsTextView.setVisibility(View.VISIBLE);
            bind.emptyDescriptionImageView.setVisibility(View.GONE);
            bind.titleEmptyDescriptionLabel.setVisibility(View.GONE);
        } else if (hasText(currentLyrics)) {
            bind.nowPlayingSongLyricsTextView.setText(MusicUtil.getReadableLyrics(currentLyrics));
            bind.nowPlayingSongLyricsTextView.setVisibility(View.VISIBLE);
            bind.emptyDescriptionImageView.setVisibility(View.GONE);
            bind.titleEmptyDescriptionLabel.setVisibility(View.GONE);
        } else if (hasText(currentDescription)) {
            bind.nowPlayingSongLyricsTextView.setText(MusicUtil.getReadableLyrics(currentDescription));
            bind.nowPlayingSongLyricsTextView.setVisibility(View.VISIBLE);
            bind.emptyDescriptionImageView.setVisibility(View.GONE);
            bind.titleEmptyDescriptionLabel.setVisibility(View.GONE);
        } else {
            bind.nowPlayingSongLyricsTextView.setVisibility(View.GONE);
            bind.emptyDescriptionImageView.setVisibility(View.VISIBLE);
            bind.titleEmptyDescriptionLabel.setVisibility(View.VISIBLE);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean hasStructuredLyrics(LyricsList lyricsList) {
        return lyricsList != null
                && lyricsList.getStructuredLyrics() != null
                && !lyricsList.getStructuredLyrics().isEmpty()
                && lyricsList.getStructuredLyrics().get(0) != null
                && lyricsList.getStructuredLyrics().get(0).getLine() != null
                && !lyricsList.getStructuredLyrics().get(0).getLine().isEmpty();
    }

    @SuppressLint("DefaultLocale")
    private void setSyncLyrics(LyricsList lyricsList) {
        if (lyricsList.getStructuredLyrics() != null && !lyricsList.getStructuredLyrics().isEmpty() && lyricsList.getStructuredLyrics().get(0).getLine() != null) {
            StringBuilder lyricsBuilder = new StringBuilder();
            List<Line> lines = lyricsList.getStructuredLyrics().get(0).getLine();

            if (lines != null) {
                for (Line line : lines) {
                    lyricsBuilder.append(line.getValue().trim()).append("\n\n");
                }
            }

            bind.nowPlayingSongLyricsTextView.setText(lyricsBuilder.toString());
        }
    }

    private void defineProgressHandler() {
        playerBottomSheetViewModel.getLiveLyricsList().observe(getViewLifecycleOwner(), lyricsList -> {
            releaseHandler();
            if (!hasStructuredLyrics(lyricsList)) {
                return;
            }

            if (!lyricsList.getStructuredLyrics().get(0).getSynced()) {
                return;
            }

            syncLyricsHandler = new Handler(Looper.getMainLooper());
            syncLyricsRunnable = new Runnable() {
                @Override
                public void run() {
                    if (syncLyricsHandler != null) {
                        if (bind != null) {
                            displaySyncedLyrics();
                        }
                        syncLyricsHandler.postDelayed(this, 250);
                    }
                }
            };

            syncLyricsHandler.postDelayed(syncLyricsRunnable, 250);
        });
    }

    private void displaySyncedLyrics() {
        if (bind == null || mediaBrowser == null) {
            return;
        }
        
        LyricsList lyricsList = playerBottomSheetViewModel.getLiveLyricsList().getValue();
        long timestamp = mediaBrowser.getCurrentPosition();

        if (hasStructuredLyrics(lyricsList)) {
            List<Line> lines = lyricsList.getStructuredLyrics().get(0).getLine();
            if (lines == null || lines.isEmpty()) {
                return;
            }

            // Find the index of the currently playing line
            int curIdx = 0;
            for (; curIdx < lines.size(); ++curIdx) {
                Integer start = lines.get(curIdx).getStart();
                if (start != null && start > timestamp) {
                    curIdx--; // Found the first line that starts after the current timestamp
                    break;
                }
            }
            if (curIdx < 0) curIdx = 0;
            if (curIdx >= lines.size()) curIdx = lines.size() - 1;

            // Only update if the highlighted line has changed
            if (lastLineIdx != null && curIdx == lastLineIdx) {
                return;
            }
            lastLineIdx = curIdx;

            StringBuilder lyricsBuilder = new StringBuilder();
            for (Line line : lines) {
                lyricsBuilder.append(line.getValue().trim()).append("\n\n");
            }
            String lyrics = lyricsBuilder.toString();
            Spannable spannableString = new SpannableString(lyrics);

            // Make each line clickable for navigation and highlight the current one
            int offset = 0;
            int highlightStart = -1;
            for (int i = 0; i < lines.size(); ++i) {
                boolean highlight = i == curIdx;
                if (highlight) highlightStart = offset;

                int len = lines.get(i).getValue().length() + 2;
                final int lineStart = lines.get(i).getStart();
                final int finalI = i;
                spannableString.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        // Seeking to 1ms after the actual start prevents scrolling / highlighting artifacts
                        mediaBrowser.seekTo(lineStart + 1);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        if (highlight) {
                            ds.setColor(requireContext().getResources().getColor(R.color.lyricsTextColor, null));
                        } else {
                            ds.setColor(requireContext().getResources().getColor(R.color.shadowsLyricsTextColor, null));
                        }
                    }
                }, offset, Math.min(offset + len, spannableString.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                offset += len;
            }

            bind.nowPlayingSongLyricsTextView.setMovementMethod(LinkMovementMethod.getInstance());
            bind.nowPlayingSongLyricsTextView.setText(spannableString);

            // Scroll to the highlighted line, but only if there is one
            if (highlightStart >= 0 && playerBottomSheetViewModel.getSyncLyricsState()) {
                bind.nowPlayingSongLyricsSrollView.smoothScrollTo(0, getScroll(highlightStart));
            }
        }
    }

    private int getScroll(int startIndex) {
        if (bind == null || bind.nowPlayingSongLyricsTextView.getLayout() == null) return 0;
        
        Layout layout = bind.nowPlayingSongLyricsTextView.getLayout();
        int line = layout.getLineForOffset(startIndex);
        int lineTop = layout.getLineTop(line);
        int lineBottom = layout.getLineBottom(line);
        int lineCenter = (lineTop + lineBottom) / 2;

        int scrollViewHeight = bind.nowPlayingSongLyricsSrollView.getHeight();
        int scroll = lineCenter - scrollViewHeight / 2;

        return Math.max(scroll, 0);
    }
}

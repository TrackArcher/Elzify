package com.elzify.music.ui.fragment;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;

import com.elzify.music.databinding.InnerFragmentPlayerCoverBinding;
import com.elzify.music.glide.CustomGlideRequest;
import com.elzify.music.service.MediaService;
import com.elzify.music.util.Constants;
import com.elzify.music.util.UIUtil;
import com.elzify.music.viewmodel.PlayerBottomSheetViewModel;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Objects;

@UnstableApi
public class PlayerCoverFragment extends Fragment {
    private PlayerBottomSheetViewModel playerBottomSheetViewModel;
    private InnerFragmentPlayerCoverBinding bind;
    private ListenableFuture<MediaBrowser> mediaBrowserListenableFuture;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        bind = InnerFragmentPlayerCoverBinding.inflate(inflater, container, false);
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
    public void onStart() {
        super.onStart();
        initializeBrowser();
        bindMediaController();
        applyPlayerBackgroundColor();
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
    }

    private void initializeBrowser() {
        mediaBrowserListenableFuture = new MediaBrowser.Builder(requireContext(), new SessionToken(requireContext(), new ComponentName(requireContext(), MediaService.class))).buildAsync();
    }

    private void releaseBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture);
    }

    private void bindMediaController() {
        mediaBrowserListenableFuture.addListener(() -> {
            try {
                MediaBrowser mediaBrowser = mediaBrowserListenableFuture.get();
                setMediaBrowserListener(mediaBrowser);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void setMediaBrowserListener(MediaBrowser mediaBrowser) {
        setCover(mediaBrowser.getMediaMetadata());

        mediaBrowser.addListener(new Player.Listener() {
            @Override
            public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
                setCover(mediaMetadata);
            }
        });
    }

    private void setCover(MediaMetadata mediaMetadata) {
        if (mediaMetadata.extras == null) return;

        String type = mediaMetadata.extras.getString("type");
        String coverArtId = mediaMetadata.extras.getString("coverArtId");
        String homepageUrl = mediaMetadata.extras.getString("homepageUrl");

        if (Objects.equals(type, Constants.MEDIA_TYPE_RADIO)
                && homepageUrl != null
                && !homepageUrl.trim().isEmpty()
                && (homepageUrl.startsWith("http://") || homepageUrl.startsWith("https://"))) {
            CustomGlideRequest.Builder
                    .from(requireContext(), homepageUrl.trim(), CustomGlideRequest.ResourceType.Radio)
                    .build()
                    .into(bind.nowPlayingSongCoverImageView);
        } else {
            CustomGlideRequest.Builder
                    .from(requireContext(), coverArtId, CustomGlideRequest.ResourceType.Song)
                    .build()
                    .into(bind.nowPlayingSongCoverImageView);
        }
    }
}

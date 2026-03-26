package com.elzify.music.ui.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elzify.music.R;
import com.elzify.music.databinding.FragmentArtistPageBinding;
import com.elzify.music.glide.CustomGlideRequest;
import com.elzify.music.helper.recyclerview.CustomLinearSnapHelper;
import com.elzify.music.helper.recyclerview.GridItemDecoration;
import com.elzify.music.interfaces.ClickCallback;
import com.elzify.music.service.MediaManager;
import com.elzify.music.service.MediaService;
import com.elzify.music.subsonic.models.ArtistID3;
import com.elzify.music.subsonic.models.Child;
import com.elzify.music.ui.activity.MainActivity;
import com.elzify.music.ui.adapter.AlbumCatalogueAdapter;
import com.elzify.music.ui.adapter.ArtistCatalogueAdapter;
import com.elzify.music.ui.adapter.SongHorizontalAdapter;
import com.elzify.music.util.Constants;
import com.elzify.music.util.MusicUtil;
import com.elzify.music.util.Preferences;
import com.elzify.music.util.TileSizeManager;
import com.elzify.music.viewmodel.ArtistPageViewModel;
import com.elzify.music.viewmodel.PlaybackViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UnstableApi
public class ArtistPageFragment extends Fragment implements ClickCallback {
    private FragmentArtistPageBinding bind;
    private MainActivity activity;
    private ArtistPageViewModel artistPageViewModel;
    private PlaybackViewModel playbackViewModel;

    private SongHorizontalAdapter songHorizontalAdapter;
    private AlbumCatalogueAdapter albumCatalogueAdapter;
    private ArtistCatalogueAdapter artistCatalogueAdapter;

    private ListenableFuture<MediaBrowser> mediaBrowserListenableFuture;

    private int spanCount = 2;
    private int tileSpacing = 20;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();

        bind = FragmentArtistPageBinding.inflate(inflater, container, false);
        View view = bind.getRoot();
        artistPageViewModel = new ViewModelProvider(requireActivity()).get(ArtistPageViewModel.class);
        playbackViewModel = new ViewModelProvider(requireActivity()).get(PlaybackViewModel.class);

        TileSizeManager.getInstance().calculateTileSize( requireContext() );
        spanCount = TileSizeManager.getInstance().getTileSpanCount( requireContext() );
        tileSpacing = TileSizeManager.getInstance().getTileSpacing( requireContext() );

        init(view);
        initAppBar();
        initArtistInfo();
        initPlayButtons();
        initTopSongsView();
        initAlbumsView();
        initSimilarArtistsView();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        initializeMediaBrowser();
        MediaManager.registerPlaybackObserver(mediaBrowserListenableFuture, playbackViewModel);
        observePlayback();
    }

    public void onResume() {
        super.onResume();
        if (songHorizontalAdapter != null) setMediaBrowserListenableFuture();
    }

    @Override
    public void onStop() {
        releaseMediaBrowser();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void init(View view) {
        artistPageViewModel.setArtist(requireArguments().getParcelable(Constants.ARTIST_OBJECT));

        bind.mostStreamedSongTextViewClickable.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.MEDIA_BY_ARTIST, Constants.MEDIA_BY_ARTIST);
            bundle.putParcelable(Constants.ARTIST_OBJECT, artistPageViewModel.getArtist());
            activity.navController.navigate(R.id.action_artistPageFragment_to_songListPageFragment, bundle);
        });

        ToggleButton favoriteToggle = view.findViewById(R.id.button_favorite);
        favoriteToggle.setChecked(artistPageViewModel.getArtist().getStarred() != null);
        favoriteToggle.setOnClickListener(v -> artistPageViewModel.setFavorite(requireContext()));

        Button bioToggle = view.findViewById(R.id.button_toggle_bio);
        bioToggle.setOnClickListener(v ->
                Toast.makeText(getActivity(), R.string.artist_no_artist_info_toast, Toast.LENGTH_SHORT).show());
    }

    private void initAppBar() {
        activity.setSupportActionBar(bind.animToolbar);
        if (activity.getSupportActionBar() != null)
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bind.collapsingToolbar.setTitle(artistPageViewModel.getArtist().getName());
        bind.animToolbar.setNavigationOnClickListener(v -> activity.navController.navigateUp());
        bind.collapsingToolbar.setExpandedTitleColor(getResources().getColor(R.color.white, null));
    }

    private void initArtistInfo() {
        artistPageViewModel.getArtistInfo(artistPageViewModel.getArtist().getId()).observe(getViewLifecycleOwner(), artistInfo -> {
            if (artistInfo == null) {
                if (bind != null) bind.artistPageBioSector.setVisibility(View.GONE);
            } else {
                if (getContext() != null && bind != null) {
                    ArtistID3 currentArtist = artistPageViewModel.getArtist();
                        String primaryId = currentArtist.getCoverArtId() != null && !currentArtist.getCoverArtId().trim().isEmpty()
                            ? currentArtist.getCoverArtId()
                            : currentArtist.getId();
                    
                    final String fallbackId = (Objects.requireNonNull(primaryId).equals(currentArtist.getCoverArtId()) &&
                                            currentArtist.getId() != null && 
                                            !currentArtist.getId().equals(primaryId))
                            ? currentArtist.getId()
                            : null;
                    
                    CustomGlideRequest.Builder
                            .from(requireContext(), primaryId, CustomGlideRequest.ResourceType.Artist)
                            .build()
                            .listener(new com.bumptech.glide.request.RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                                            Object model,
                                                            @NonNull com.bumptech.glide.request.target.Target<Drawable> target,
                                                            boolean isFirstResource) {
                                    if (e != null) {
                                        e.getMessage();
                                        if (e.getMessage().contains("400") && fallbackId != null) {

                                            Log.d("ArtistCover", "Primary ID failed (400), trying fallback: " + fallbackId);

                                            CustomGlideRequest.Builder
                                                    .from(requireContext(), fallbackId, CustomGlideRequest.ResourceType.Artist)
                                                    .build()
                                                    .into(bind.artistBackdropImageView);
                                            return true;
                                        }
                                    }
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(@NonNull Drawable resource,
                                                               @NonNull Object model,
                                                               com.bumptech.glide.request.target.Target<Drawable> target,
                                                               @NonNull com.bumptech.glide.load.DataSource dataSource,
                                                               boolean isFirstResource) {
                                    return false;
                                }
                            })
                            .into(bind.artistBackdropImageView);
                }

                if (bind != null) {
                    String normalizedBio = MusicUtil.forceReadableString(artistInfo.getBiography()).trim();
                    String lastFmUrl = artistInfo.getLastFmUrl();

                    if (normalizedBio.isEmpty()) {
                        bind.bioTextView.setVisibility(View.GONE);
                    } else {
                        bind.bioTextView.setText(normalizedBio);
                    }

                    if (lastFmUrl == null) {
                        bind.bioMoreTextViewClickable.setVisibility(View.GONE);
                    } else {
                        bind.bioMoreTextViewClickable.setOnClickListener(v -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(artistInfo.getLastFmUrl()));
                            startActivity(intent);
                        });
                        bind.bioMoreTextViewClickable.setVisibility(View.VISIBLE);
                    }

                    if (!normalizedBio.isEmpty() || lastFmUrl != null) {
                        View view = bind.getRoot();

                        Button bioToggle = view.findViewById(R.id.button_toggle_bio);
                        bioToggle.setOnClickListener(v -> {
                            if (bind != null) {
                                boolean displayBio = Preferences.getArtistDisplayBiography();
                                Preferences.setArtistDisplayBiography(!displayBio);
                                bind.artistPageBioSector.setVisibility(displayBio ? View.GONE : View.VISIBLE);
                            }
                        });

                        boolean displayBio = Preferences.getArtistDisplayBiography();
                        bind.artistPageBioSector.setVisibility(displayBio ? View.VISIBLE : View.GONE);
                    }
                }
            }
        });
    }

    private void initPlayButtons() {
        bind.artistPageShuffleButton.setOnClickListener(v -> artistPageViewModel.getArtistShuffleList().observe(getViewLifecycleOwner(), new Observer<List<Child>>() {
            @Override
            public void onChanged(List<Child> songs) {
                if (songs != null && !songs.isEmpty()) {
                    MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0);
                    activity.setBottomSheetInPeek(true);
                    artistPageViewModel.getArtistShuffleList().removeObserver(this);
                }
            }
        }));

        bind.artistPageRadioButton.setOnClickListener(v -> artistPageViewModel.getArtistInstantMix().observe(getViewLifecycleOwner(), new Observer<List<Child>>() {
            @Override
            public void onChanged(List<Child> songs) {
                if (songs != null && !songs.isEmpty()) {
                    MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0);
                    activity.setBottomSheetInPeek(true);
                    artistPageViewModel.getArtistInstantMix().removeObserver(this);
                }
            }
        }));
    }

    private void initTopSongsView() {
        bind.mostStreamedSongRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        songHorizontalAdapter = new SongHorizontalAdapter(getViewLifecycleOwner(), this, true, true, null);
        bind.mostStreamedSongRecyclerView.setAdapter(songHorizontalAdapter);
        setMediaBrowserListenableFuture();
        reapplyPlayback();
        artistPageViewModel.getArtistTopSongList().observe(getViewLifecycleOwner(), songs -> {
            if (songs == null) {
                if (bind != null) bind.artistPageTopSongsSector.setVisibility(View.GONE);
            } else {
                if (bind != null)
                    bind.artistPageTopSongsSector.setVisibility(!songs.isEmpty() ? View.VISIBLE : View.GONE);
                songHorizontalAdapter.setItems(songs);
                reapplyPlayback();
            }
        });
    }

    private void initAlbumsView() {
        bind.albumsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        bind.albumsRecyclerView.addItemDecoration(new GridItemDecoration(spanCount, tileSpacing, false));
        bind.albumsRecyclerView.setHasFixedSize(true);

        albumCatalogueAdapter = new AlbumCatalogueAdapter(this, false);
        bind.albumsRecyclerView.setAdapter(albumCatalogueAdapter);

        artistPageViewModel.getAlbumList().observe(getViewLifecycleOwner(), albums -> {
            if (albums == null) {
                if (bind != null) bind.artistPageAlbumsSector.setVisibility(View.GONE);
            } else {
                if (bind != null)
                    bind.artistPageAlbumsSector.setVisibility(!albums.isEmpty() ? View.VISIBLE : View.GONE);
                albumCatalogueAdapter.setItems(albums);
            }
        });
    }

    private void initSimilarArtistsView() {
        bind.similarArtistsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        bind.similarArtistsRecyclerView.addItemDecoration(new GridItemDecoration(spanCount, tileSpacing, false));
        bind.similarArtistsRecyclerView.setHasFixedSize(true);

        artistCatalogueAdapter = new ArtistCatalogueAdapter(this);
        bind.similarArtistsRecyclerView.setAdapter(artistCatalogueAdapter);

        artistPageViewModel.getArtistInfo(artistPageViewModel.getArtist().getId()).observe(getViewLifecycleOwner(), artist -> {
            if (artist == null) {
                if (bind != null) bind.similarArtistSector.setVisibility(View.GONE);
            } else {
                if (bind != null && artist.getSimilarArtists() != null)
                    bind.similarArtistSector.setVisibility(!artist.getSimilarArtists().isEmpty() ? View.VISIBLE : View.GONE);

                List<ArtistID3> artists = new ArrayList<>();

                if (artist.getSimilarArtists() != null) {
                    artists.addAll(artist.getSimilarArtists());
                }

                artistCatalogueAdapter.setItems(artists);
            }
        });

        CustomLinearSnapHelper similarArtistSnapHelper = new CustomLinearSnapHelper();
        similarArtistSnapHelper.attachToRecyclerView(bind.similarArtistsRecyclerView);
    }

    private void initializeMediaBrowser() {
        mediaBrowserListenableFuture = new MediaBrowser.Builder(requireContext(), new SessionToken(requireContext(), new ComponentName(requireContext(), MediaService.class))).buildAsync();
    }

    private void releaseMediaBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture);
    }

    @Override
    public void onMediaClick(Bundle bundle) {
        MediaManager.startQueue(mediaBrowserListenableFuture, bundle.getParcelableArrayList(Constants.TRACKS_OBJECT), bundle.getInt(Constants.ITEM_POSITION));
        activity.setBottomSheetInPeek(true);
    }

    @Override
    public void onMediaLongClick(Bundle bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle);
    }

    @Override
    public void onAlbumClick(Bundle bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.albumPageFragment, bundle);
    }

    @Override
    public void onAlbumLongClick(Bundle bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle);
    }

    @Override
    public void onArtistClick(Bundle bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistPageFragment, bundle);
    }

    @Override
    public void onArtistLongClick(Bundle bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, bundle);
    }

    private void observePlayback() {
        playbackViewModel.getCurrentSongId().observe(getViewLifecycleOwner(), id -> {
            if (songHorizontalAdapter != null) {
                Boolean playing = playbackViewModel.getIsPlaying().getValue();
                songHorizontalAdapter.setPlaybackState(id, playing != null && playing);
            }
        });
        playbackViewModel.getIsPlaying().observe(getViewLifecycleOwner(), playing -> {
            if (songHorizontalAdapter != null) {
                String id = playbackViewModel.getCurrentSongId().getValue();
                songHorizontalAdapter.setPlaybackState(id, playing != null && playing);
            }
        });
    }

    private void reapplyPlayback() {
        if (songHorizontalAdapter != null) {
            String id = playbackViewModel.getCurrentSongId().getValue();
            Boolean playing = playbackViewModel.getIsPlaying().getValue();
            songHorizontalAdapter.setPlaybackState(id, playing != null && playing);
        }
    }

    private void setMediaBrowserListenableFuture() {
        songHorizontalAdapter.setMediaBrowserListenableFuture(mediaBrowserListenableFuture);
    }
}
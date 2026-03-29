package com.elzify.music.ui.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elzify.music.R;
import com.elzify.music.databinding.ItemHorizontalPlaylistDialogBinding;
import com.elzify.music.interfaces.ClickCallback;
import com.elzify.music.subsonic.models.Playlist;
import com.elzify.music.util.Constants;
import com.elzify.music.util.MusicUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistDialogHorizontalAdapter extends RecyclerView.Adapter<PlaylistDialogHorizontalAdapter.ViewHolder> {
    private final ClickCallback click;

    private List<Playlist> playlists;
    private List<Playlist> allPlaylists;
    private final java.util.Set<String> selectedIds = new java.util.HashSet<>();

    public PlaylistDialogHorizontalAdapter(ClickCallback click) {
        this.click = click;
        this.playlists = Collections.emptyList();
        this.allPlaylists = Collections.emptyList();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHorizontalPlaylistDialogBinding view = ItemHorizontalPlaylistDialogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);

        holder.item.playlistDialogTitleTextView.setText(playlist.getName());
        holder.item.playlistDialogCountTextView.setText(holder.itemView.getContext().getString(R.string.playlist_counted_tracks, playlist.getSongCount(), MusicUtil.getReadableDurationString(playlist.getDuration(), false)));

        holder.item.playlistDialogCheckbox.setOnCheckedChangeListener(null);
        holder.item.playlistDialogCheckbox.setChecked(selectedIds.contains(playlist.getId()));
        holder.item.playlistDialogCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedIds.add(playlist.getId());
            } else {
                selectedIds.remove(playlist.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public List<String> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    public void setItems(List<Playlist> playlists) {
        this.allPlaylists = playlists;
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (query == null || query.isEmpty()) {
            playlists = allPlaylists;
        } else {
            List<Playlist> filteredList = new ArrayList<>();
            for (Playlist playlist : allPlaylists) {
                if (playlist.getName() != null && playlist.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(playlist);
                }
            }
            playlists = filteredList;
        }
        notifyDataSetChanged();
    }

    public Playlist getItem(int id) {
        return playlists.get(id);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ItemHorizontalPlaylistDialogBinding item;

        ViewHolder(ItemHorizontalPlaylistDialogBinding item) {
            super(item.getRoot());

            this.item = item;

            item.playlistDialogTitleTextView.setSelected(true);

            itemView.setOnClickListener(v -> {
                item.playlistDialogCheckbox.toggle();
            });
        }
    }
}

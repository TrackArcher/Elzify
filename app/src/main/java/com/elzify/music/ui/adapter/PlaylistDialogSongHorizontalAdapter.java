package com.elzify.music.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elzify.music.databinding.ItemHorizontalPlaylistDialogTrackBinding;
import com.elzify.music.glide.CustomGlideRequest;
import com.elzify.music.subsonic.models.Child;
import com.elzify.music.util.MusicUtil;

import java.util.Collections;
import java.util.List;

public class PlaylistDialogSongHorizontalAdapter extends RecyclerView.Adapter<PlaylistDialogSongHorizontalAdapter.ViewHolder> {
    private List<Child> songs;

    public PlaylistDialogSongHorizontalAdapter() {
        this.songs = Collections.emptyList();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHorizontalPlaylistDialogTrackBinding view = ItemHorizontalPlaylistDialogTrackBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Child song = songs.get(position);

        holder.item.playlistDialogSongTitleTextView.setText(song.getTitle());
        holder.item.playlistDialogAlbumArtistTextView.setText(song.getArtist());
        holder.item.playlistDialogSongDurationTextView.setText(MusicUtil.getReadableDurationString(song.getDuration(), false));

        if (song.getStarred() == null && (song.getUserRating() == null || song.getUserRating() == 0)) {
            holder.item.ratingIndicatorImageView.setVisibility(android.view.View.GONE);
        } else {
            holder.item.ratingIndicatorImageView.setVisibility(android.view.View.VISIBLE);
            holder.item.preferredIcon.setVisibility(song.getStarred() != null ? android.view.View.VISIBLE : android.view.View.GONE);
            holder.item.ratingBarLayout.setVisibility(song.getUserRating() != null && song.getUserRating() > 0 ? android.view.View.VISIBLE : android.view.View.GONE);

            if (song.getUserRating() != null && song.getUserRating() > 0) {
                holder.item.oneStarIcon.setImageDrawable(androidx.appcompat.content.res.AppCompatResources.getDrawable(holder.itemView.getContext(), song.getUserRating() >= 1 ? com.cappielloantonio.tempo.R.drawable.ic_star : com.cappielloantonio.tempo.R.drawable.ic_star_outlined));
                holder.item.twoStarIcon.setImageDrawable(androidx.appcompat.content.res.AppCompatResources.getDrawable(holder.itemView.getContext(), song.getUserRating() >= 2 ? com.cappielloantonio.tempo.R.drawable.ic_star : com.cappielloantonio.tempo.R.drawable.ic_star_outlined));
                holder.item.threeStarIcon.setImageDrawable(androidx.appcompat.content.res.AppCompatResources.getDrawable(holder.itemView.getContext(), song.getUserRating() >= 3 ? com.cappielloantonio.tempo.R.drawable.ic_star : com.cappielloantonio.tempo.R.drawable.ic_star_outlined));
                holder.item.fourStarIcon.setImageDrawable(androidx.appcompat.content.res.AppCompatResources.getDrawable(holder.itemView.getContext(), song.getUserRating() >= 4 ? com.cappielloantonio.tempo.R.drawable.ic_star : com.cappielloantonio.tempo.R.drawable.ic_star_outlined));
                holder.item.fiveStarIcon.setImageDrawable(androidx.appcompat.content.res.AppCompatResources.getDrawable(holder.itemView.getContext(), song.getUserRating() >= 5 ? com.cappielloantonio.tempo.R.drawable.ic_star : com.cappielloantonio.tempo.R.drawable.ic_star_outlined));
            }
        }

        CustomGlideRequest.Builder
                .from(holder.itemView.getContext(), song.getCoverArtId(), CustomGlideRequest.ResourceType.Song)
                .build()
                .into(holder.item.playlistDialogSongCoverImageView);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public List<Child> getItems() {
        return this.songs;
    }

    public void setItems(List<Child> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    public Child getItem(int id) {
        return songs.get(id);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemHorizontalPlaylistDialogTrackBinding item;

        ViewHolder(ItemHorizontalPlaylistDialogTrackBinding item) {
            super(item.getRoot());

            this.item = item;

            item.playlistDialogSongTitleTextView.setSelected(true);
        }
    }
}

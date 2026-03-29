package com.elzify.music.ui.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elzify.music.databinding.ItemAlbumCarouselBinding;
import com.elzify.music.glide.CustomGlideRequest;
import com.elzify.music.interfaces.ClickCallback;
import com.elzify.music.subsonic.models.AlbumID3;
import com.elzify.music.util.Constants;

import java.util.Collections;
import java.util.List;

public class AlbumCarouselAdapter extends RecyclerView.Adapter<AlbumCarouselAdapter.ViewHolder> {
    private final ClickCallback click;
    private final boolean isOffline;

    private List<AlbumID3> albums;

    public AlbumCarouselAdapter(ClickCallback click, boolean isOffline) {
        this.click = click;
        this.isOffline = isOffline;
        this.albums = Collections.emptyList();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlbumCarouselBinding view = ItemAlbumCarouselBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AlbumID3 album = albums.get(position);

        holder.item.albumNameLabel.setText(album.getName());
        holder.item.artistNameLabel.setText(album.getArtist());

        CustomGlideRequest.Builder
                .from(holder.itemView.getContext(), album.getCoverArtId(), CustomGlideRequest.ResourceType.Album)
                .build()
                .into(holder.item.albumCoverImageView);
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public void setItems(List<AlbumID3> albums) {
        this.albums = albums != null ? albums : Collections.emptyList();
        notifyDataSetChanged();
    }

    public AlbumID3 getItem(int position) {
        return albums.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ItemAlbumCarouselBinding item;

        ViewHolder(ItemAlbumCarouselBinding item) {
            super(item.getRoot());

            this.item = item;

            item.albumNameLabel.setSelected(true);
            item.artistNameLabel.setSelected(true);

            itemView.setOnClickListener(v -> onClick());
            itemView.setOnLongClickListener(v -> onLongClick());
        }

        private void onClick() {
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.ALBUM_OBJECT, albums.get(getBindingAdapterPosition()));

            click.onAlbumClick(bundle);
        }

        private boolean onLongClick() {
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.ALBUM_OBJECT, albums.get(getBindingAdapterPosition()));

            click.onAlbumLongClick(bundle);

            return true;
        }
    }
}

package com.elzify.music.ui.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elzify.music.databinding.ItemAlbumCarouselBinding;
import com.elzify.music.glide.CustomGlideRequest;
import com.elzify.music.interfaces.ClickCallback;
import com.elzify.music.subsonic.models.AlbumID3;
import com.elzify.music.util.Constants;
import com.elzify.music.util.TileSizeManager;

import java.util.Collections;
import java.util.List;

public class AlbumCarouselAdapter extends RecyclerView.Adapter<AlbumCarouselAdapter.ViewHolder> {
    private final ClickCallback click;

    private List<AlbumID3> albums;
    private boolean showArtist;
    private int sizePx = 400;

    // Keep your custom 'showArtist' logic, but optimize the size calculation
    public AlbumCarouselAdapter(ClickCallback click, boolean showArtist, Context context) {
        this.click = click;
        this.showArtist = showArtist;
        this.albums = Collections.emptyList();

        // Calculate size ONCE in the constructor, not every time a view is created
        TileSizeManager.getInstance().calculateTileSize(context);
        this.sizePx = TileSizeManager.getInstance().getTileSizePx(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlbumCarouselBinding view = ItemAlbumCarouselBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        // Use the pre-calculated sizePx inside the ViewHolder
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ViewGroup.LayoutParams lp = holder.item.albumCoverImageView.getLayoutParams();
        lp.width = sizePx;
        lp.height = sizePx;
        holder.item.albumCoverImageView.setLayoutParams(lp);

        AlbumID3 album = albums.get(position);

        holder.item.albumNameLabel.setText(album.getName());
        holder.item.artistNameLabel.setText(album.getArtist());
        holder.item.artistNameLabel.setVisibility(showArtist ? View.VISIBLE : View.GONE);

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
        this.albums = albums;
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

            itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constants.ALBUM_OBJECT, albums.get(getBindingAdapterPosition()));
                click.onAlbumClick(bundle);
            });

            itemView.setOnLongClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constants.ALBUM_OBJECT, albums.get(getBindingAdapterPosition()));
                click.onAlbumLongClick(bundle);
                return true;
            });

            item.albumNameLabel.setSelected(true);
            item.artistNameLabel.setSelected(true);
        }
    }
}

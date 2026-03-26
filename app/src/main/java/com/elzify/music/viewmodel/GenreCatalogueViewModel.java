package com.elzify.music.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.elzify.music.repository.GenreRepository;
import com.elzify.music.subsonic.models.Genre;

import java.util.List;

public class GenreCatalogueViewModel extends AndroidViewModel {
    private final GenreRepository genreRepository;

    public GenreCatalogueViewModel(@NonNull Application application) {
        super(application);

        genreRepository = new GenreRepository();
    }

    public LiveData<List<Genre>> getGenreList() {
        return genreRepository.getGenres(false, -1);
    }
}

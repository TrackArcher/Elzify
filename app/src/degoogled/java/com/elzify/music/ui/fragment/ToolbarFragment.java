package com.cappielloantonio.tempo.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;

import com.cappielloantonio.tempo.databinding.FragmentToolbarBinding;
import com.cappielloantonio.tempo.ui.activity.MainActivity;

@UnstableApi
public class ToolbarFragment extends Fragment {
    private static final String TAG = "ToolbarFragment";

    private FragmentToolbarBinding bind;
    private MainActivity activity;

    public ToolbarFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();

        bind = FragmentToolbarBinding.inflate(inflater, container, false);
        View view = bind.getRoot();

        return view;
    }
}
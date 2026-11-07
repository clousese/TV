package com.fongmi.android.tv.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.OneShotPreDrawListener;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Tracks;
import androidx.media3.ui.DefaultTrackNameProvider;
import androidx.media3.ui.TrackNameProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.databinding.DialogTrackBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.track.TrackUtil;
import com.fongmi.android.tv.ui.adapter.TrackAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

import java.util.ArrayList;
import java.util.List;

public final class TrackDialog extends BaseBottomSheetDialog implements TrackAdapter.OnClickListener {

    private final TrackNameProvider provider;
    private DialogTrackBinding binding;
    private PlayerManager player;
    private int type;

    public TrackDialog() {
        this.provider = new DefaultTrackNameProvider(App.get().getResources());
    }

    public static TrackDialog create() {
        return new TrackDialog();
    }

    public TrackDialog type(int type) {
        Bundle arguments = new Bundle();
        arguments.putInt("type", type);
        setArguments(arguments);
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = requireArguments().getInt("type");
    }

    public void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof TrackDialog) return;
        showNow(manager, null);
    }

    private boolean hasSetting() {
        return type == C.TRACK_TYPE_AUDIO || type == C.TRACK_TYPE_VIDEO;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogTrackBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        PlaybackDialog.observe(this, this::bindPlayer);
    }

    private void bindPlayer(PlayerManager player) {
        this.player = player;
        if (player == null) return;
        setRecyclerView();
        if (hasSetting()) {
            binding.setting.setVisibility(View.VISIBLE);
            binding.setting.setContentDescription(getString(getSettingText()));
        } else {
            binding.setting.setVisibility(View.GONE);
        }
        binding.title.setText(ResUtil.getStringArray(R.array.select_track)[type - 1]);
    }

    private int getSettingText() {
        return switch (type) {
            case C.TRACK_TYPE_AUDIO -> R.string.audio_setting;
            case C.TRACK_TYPE_VIDEO -> R.string.video_setting;
            default -> R.string.audio_setting;
        };
    }

    @Override
    protected void initEvent() {
        binding.setting.setOnClickListener(this::onSetting);
    }

    private void setRecyclerView() {
        TrackAdapter adapter = new TrackAdapter(this);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter.addAll(getTrack()));
        binding.recycler.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        if (adapter.getItemCount() == 0) return;
        int selected = adapter.getSelected();
        binding.recycler.getLayoutManager().scrollToPosition(selected);
        if (Util.isLeanback()) {
            binding.getRoot().setFocusableInTouchMode(true);
            binding.getRoot().requestFocus();
            OneShotPreDrawListener.add(binding.recycler, () -> onTrackPreDraw(selected));
        }
    }

    private void onTrackPreDraw(int selected) {
        View view = binding.recycler.getLayoutManager().findViewByPosition(selected);
        if (view == null || !view.requestFocus()) binding.recycler.scrollToPosition(selected);
        binding.getRoot().setFocusableInTouchMode(false);
        binding.getRoot().setFocusable(false);
    }

    private void onSetting(View view) {
        FragmentActivity activity = requireActivity();
        dismissNow();
        showSetting(activity);
    }

    private void showSetting(FragmentActivity activity) {
        switch (type) {
            case C.TRACK_TYPE_AUDIO -> AudioSettingDialog.create().show(activity);
            case C.TRACK_TYPE_VIDEO -> VideoSettingDialog.create().show(activity);
        }
    }

    private List<TrackAdapter.TrackItem> getTrack() {
        List<TrackAdapter.TrackItem> items = new ArrayList<>();
        int ordinal = 0;
        for (Tracks.Group trackGroup : player.getCurrentTracks().getGroups()) {
            if (trackGroup.getType() != type) continue;
            for (int j = 0; j < trackGroup.length; j++) items.add(createTrackItem(trackGroup, j, ordinal++));
        }
        return items;
    }

    private TrackAdapter.TrackItem createTrackItem(Tracks.Group trackGroup, int index, int ordinal) {
        Format format = trackGroup.getTrackFormat(index);
        boolean selected = trackGroup.isTrackSelected(index);
        String name = provider.getTrackName(format);
        Track track = TrackUtil.createTrack(type, name, format, ordinal);
        track.setSelected(selected);
        track.setRole(Track.ROLE_PRIMARY);
        return new TrackAdapter.TrackItem(track, 0);
    }

    @Override
    public void onItemClick(Track item) {
        if (!PlaybackDialog.isCurrentPlayer(this, player)) return;
        player.setTrack(item.key(player.getKey()));
        dismiss();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        player = null;
        super.onDestroyView();
    }
}

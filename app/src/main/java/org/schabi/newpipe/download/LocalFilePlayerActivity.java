package org.schabi.newpipe.download;

import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

/** Plays completed downloads inside BearTube without leaving the app. */
public class LocalFilePlayerActivity extends AppCompatActivity {
    private ExoPlayer player;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Uri fileUri = getIntent().getData();
        if (fileUri == null) {
            finish();
            return;
        }

        setTitle(getIntent().getStringExtra(IntentExtras.TITLE));

        final PlayerView playerView = new PlayerView(this);
        playerView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        playerView.setKeepScreenOn(true);
        playerView.setUseController(true);
        setContentView(playerView);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(final PlaybackException error) {
                finish();
            }
        });
        player.setMediaItem(MediaItem.fromUri(fileUri));
        player.prepare();
        player.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    public static final class IntentExtras {
        public static final String TITLE = "download_title";

        private IntentExtras() {
        }
    }
}

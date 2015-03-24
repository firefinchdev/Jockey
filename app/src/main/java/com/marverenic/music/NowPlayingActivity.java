package com.marverenic.music;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

public class NowPlayingActivity extends Activity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private MediaObserver observer = null;
    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };
    private boolean userTouchingProgressBar = false; // This probably shouldn't be here...

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) //Don't worry Lint. Everything is going to be okay.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getResources().getConfiguration().smallestScreenWidthDp >= 700) {
            // If the activity is landscape on a tablet, use a different theme
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Themes.setTheme(this);
                getWindow().setStatusBarColor(Themes.getPrimaryDark());
                if (getActionBar() != null) {
                    getActionBar().setDisplayHomeAsUpEnabled(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) getActionBar().setElevation(getResources().getDimension(R.dimen.header_elevation));
                }
            }
            else {
                // If the activity is in portrait, use the default theme
                setTheme(R.style.NowPlayingTheme);
                if (getActionBar() != null) {
                    getActionBar().setDisplayHomeAsUpEnabled(true);
                    getActionBar().setTitle("");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) getActionBar().setElevation(0);
                }
            }
        } else {
            // For devices that aren't tablets
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (getActionBar() != null) {
                getActionBar().setDisplayHomeAsUpEnabled(true);
                getActionBar().setTitle("");
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        Themes.themeActivity(R.layout.activity_now_playing, getWindow().getDecorView().findViewById(android.R.id.content), this);

        findViewById(R.id.playButton).setOnClickListener(this);
        findViewById(R.id.nextButton).setOnClickListener(this);
        findViewById(R.id.previousButton).setOnClickListener(this);
        ((SeekBar) findViewById(R.id.songSeekBar)).setOnSeekBarChangeListener(this);

        observer = new MediaObserver(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.now_playing, menu);

        if (PlayerService.isShuffle()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                menu.getItem(0).setIcon(R.drawable.ic_vector_shuffle);
            else
                menu.getItem(0).setIcon(R.drawable.ic_shuffle);

            menu.getItem(0).setTitle("Disable Shuffle");
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                menu.getItem(0).setIcon(R.drawable.ic_vector_shuffle_off);
            else
                menu.getItem(0).setIcon(R.drawable.ic_shuffle_off);

            menu.getItem(0).setTitle("Enable Shuffle");
        }

        if (PlayerService.isRepeat()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                menu.getItem(1).setIcon(R.drawable.ic_vector_repeat);
            else
                menu.getItem(1).setIcon(R.drawable.ic_repeat);

            menu.getItem(1).setTitle("Enable Repeat One");
        } else {
            if (PlayerService.isRepeatOne()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    menu.getItem(1).setIcon(R.drawable.ic_vector_repeat_one);
                else
                    menu.getItem(1).setIcon(R.drawable.ic_repeat_one);
                menu.getItem(1).setTitle("Disable Repeat");
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    menu.getItem(1).setIcon(R.drawable.ic_vector_repeat_off);
                else
                    menu.getItem(1).setIcon(R.drawable.ic_repeat_off);
                menu.getItem(1).setTitle("Enable Repeat");
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Navigate.up(this);
                return true;
            case R.id.action_shuffle:
                PlayerService.toggleShuffle();
                if (PlayerService.isShuffle()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        item.setIcon(R.drawable.ic_vector_shuffle);
                    else
                        item.setIcon(R.drawable.ic_shuffle);

                    item.setTitle("Disable Shuffle");
                    Toast toast = Toast.makeText(this, "Shuffle Enabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        item.setIcon(R.drawable.ic_vector_shuffle_off);
                    else
                        item.setIcon(R.drawable.ic_shuffle_off);

                    item.setTitle("Enable Shuffle");
                    Toast toast = Toast.makeText(this, "Shuffle Disabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                return true;
            case R.id.action_repeat:
                PlayerService.toggleRepeat();
                if (PlayerService.isRepeat()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        item.setIcon(R.drawable.ic_vector_repeat);
                    else
                        item.setIcon(R.drawable.ic_repeat);

                    item.setTitle("Enable Repeat One");
                    Toast toast = Toast.makeText(this, "Repeat Enabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    if (PlayerService.isRepeatOne()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            item.setIcon(R.drawable.ic_vector_repeat_one);
                        else
                            item.setIcon(R.drawable.ic_repeat_one);

                        item.setTitle("Disable Repeat");
                        Toast toast = Toast.makeText(this, "Repeat One Enabled", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            item.setIcon(R.drawable.ic_vector_repeat_off);
                        else
                            item.setIcon(R.drawable.ic_repeat_off);

                        item.setTitle("Enable Repeat");
                        Toast toast = Toast.makeText(this, "Repeat Disabled", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
                return true;
            case R.id.action_queue:
                Navigate.to(this, QueueActivity.class);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        observer.stop();
        unregisterReceiver(updateReceiver);
        LibraryScanner.saveLibrary(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        new Thread(observer).start();
        update();
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.playButton) {
            if (PlayerService.getNowPlaying() != null) {
                PlayerService.togglePlay();
            }
        } else if (v.getId() == R.id.nextButton) {
            PlayerService.skip();
        } else if (v.getId() == R.id.previousButton) {
            PlayerService.previous();
        }
        else if (v.getId() == R.id.songInfo){
            final Context context = this;
            final Song nowPlaying = PlayerService.getNowPlaying();

            if (nowPlaying != null) {
                new AlertDialog.Builder(context, Themes.getAlertTheme(context))
                        .setTitle(nowPlaying.songName)
                        .setNegativeButton("Cancel", null)
                        .setItems(R.array.now_playing_options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: //Go to artist
                                        Artist artist;

                                        Cursor curArtist = getContentResolver().query(
                                                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                                                null,
                                                MediaStore.Audio.Media.ARTIST + " =?",
                                                new String[]{nowPlaying.artistName},
                                                MediaStore.Audio.Artists.ARTIST + " ASC");
                                        curArtist.moveToFirst();

                                        artist = new Artist(
                                                curArtist.getLong(curArtist.getColumnIndex(MediaStore.Audio.Artists._ID)),
                                                curArtist.getString(curArtist.getColumnIndex(MediaStore.Audio.Artists.ARTIST)));

                                        curArtist.close();

                                        Navigate.to(context, LibraryPageActivity.class, "entry", artist);
                                        break;
                                    case 1: //Go to album
                                        Album album;

                                        Cursor curAlbum = getContentResolver().query(
                                                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                                null,
                                                MediaStore.Audio.Media.ALBUM + " =? AND " + MediaStore.Audio.Media.ARTIST + " =?",
                                                new String[]{nowPlaying.albumName, nowPlaying.artistName},
                                                MediaStore.Audio.Albums.ALBUM + " ASC");
                                        curAlbum.moveToFirst();

                                        album = new Album(
                                                curAlbum.getLong(curAlbum.getColumnIndex(MediaStore.Audio.Albums._ID)),
                                                curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                                                curAlbum.getLong(curAlbum.getColumnIndex(MediaStore.Audio.Artists._ID)),
                                                curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                                                curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)),
                                                curAlbum.getString(curAlbum.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)));

                                        curAlbum.close();

                                        Navigate.to(context, LibraryPageActivity.class, "entry", album);
                                        break;
                                    case 2: //Add to playlist
                                        ArrayList<Playlist> playlists = Library.getPlaylists();
                                        String[] playlistNames = new String[playlists.size()];

                                        for (int i = 0; i < playlists.size(); i++ ){
                                            playlistNames[i] = playlists.get(i).toString();
                                        }

                                        new AlertDialog.Builder(context, Themes.getAlertTheme(context))
                                                .setTitle("Add \"" + nowPlaying.songName + "\" to playlist")
                                                .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        LibraryScanner.addPlaylistEntry(context, Library.getPlaylists().get(which), nowPlaying);
                                                    }
                                                })
                                                .setNeutralButton("Cancel", null)
                                                .show();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        })
                        .show();
            }
        }
        update();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void update() {
        if (PlayerService.getNowPlaying() != null) {

            //final ViewGroup background = (ViewGroup) findViewById(R.id.playerControlFrame);
            final TextView songTitle = (TextView) findViewById(R.id.textSongTitle);
            final TextView artistName = (TextView) findViewById(R.id.textArtistName);
            final TextView albumTitle = (TextView) findViewById(R.id.textAlbumTitle);
            final SeekBar seekBar = ((SeekBar) findViewById(R.id.songSeekBar));

            songTitle.setText(PlayerService.getNowPlaying().songName);
            artistName.setText(PlayerService.getNowPlaying().artistName);
            albumTitle.setText(PlayerService.getNowPlaying().albumName);

            ImageView artImageView = (ImageView) findViewById(R.id.imageArtwork);
            if (PlayerService.getFullArt() != null) {
                artImageView.setImageBitmap(PlayerService.getFullArt());
            }
            else if (PlayerService.getArt() != null){
                artImageView.setImageBitmap(PlayerService.getArt());
            }
            else {
                if (getResources().getConfiguration().smallestScreenWidthDp >= 700) {
                    artImageView.setImageResource(R.drawable.art_default_xxl);
                } else {
                    artImageView.setImageResource(R.drawable.art_default_xl);
                }
            }

            if ((PlayerService.isPlaying() || PlayerService.isPreparing())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_pause_circle_fill);
                } else {
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_circle_fill);
                }
                if (!PlayerService.isPreparing()) {
                    if (!observer.isRunning()) new Thread(observer).start();
                    seekBar.setMax(PlayerService.getNowPlaying().songDuration);
                }
                else{
                    observer.stop();
                    seekBar.setProgress(0);
                    seekBar.setMax(Integer.MAX_VALUE);
                }
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_play_circle_fill);
                } else {
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_circle_fill);
                }
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && !userTouchingProgressBar) {
            // For keyboards and non-touch based things
            onStartTrackingTouch(seekBar);
            onStopTrackingTouch(seekBar);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        observer.stop();
        userTouchingProgressBar = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        PlayerService.seek(seekBar.getProgress());
        observer = new MediaObserver(this);
        new Thread(observer).start();
        userTouchingProgressBar = false;

    }

    private class MediaObserver implements Runnable {
        private boolean stop = false;
        private SeekBar progress;
        private NowPlayingActivity parent;

        public MediaObserver(NowPlayingActivity parent) {
            progress = (SeekBar) findViewById(R.id.songSeekBar);
            this.parent = parent;
        }

        public void stop() {
            stop = true;
        }

        @Override
        public void run() {
            stop = false;
            while (!stop) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setProgress(PlayerService.getCurrentPosition());
                    }
                });
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    e.printStackTrace();
                    Debug.log(Debug.LogLevel.WTF, "NowPlayingActivity/MediaObserver", "Some horrible thread exception has occurred", parent);
                }
            }
        }

        public boolean isRunning() {
            return !stop;
        }
    }
}

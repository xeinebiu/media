/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2;

import static com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO;
import static com.google.android.exoplayer2.C.TRACK_TYPE_CAMERA_MOTION;
import static com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO;
import static com.google.android.exoplayer2.Player.COMMAND_ADJUST_DEVICE_VOLUME;
import static com.google.android.exoplayer2.Player.COMMAND_CHANGE_MEDIA_ITEMS;
import static com.google.android.exoplayer2.Player.COMMAND_GET_AUDIO_ATTRIBUTES;
import static com.google.android.exoplayer2.Player.COMMAND_GET_CURRENT_MEDIA_ITEM;
import static com.google.android.exoplayer2.Player.COMMAND_GET_DEVICE_VOLUME;
import static com.google.android.exoplayer2.Player.COMMAND_GET_MEDIA_ITEMS_METADATA;
import static com.google.android.exoplayer2.Player.COMMAND_GET_TEXT;
import static com.google.android.exoplayer2.Player.COMMAND_GET_TIMELINE;
import static com.google.android.exoplayer2.Player.COMMAND_GET_TRACK_INFOS;
import static com.google.android.exoplayer2.Player.COMMAND_GET_VOLUME;
import static com.google.android.exoplayer2.Player.COMMAND_PLAY_PAUSE;
import static com.google.android.exoplayer2.Player.COMMAND_PREPARE;
import static com.google.android.exoplayer2.Player.COMMAND_SEEK_TO_DEFAULT_POSITION;
import static com.google.android.exoplayer2.Player.COMMAND_SEEK_TO_MEDIA_ITEM;
import static com.google.android.exoplayer2.Player.COMMAND_SET_DEVICE_VOLUME;
import static com.google.android.exoplayer2.Player.COMMAND_SET_MEDIA_ITEMS_METADATA;
import static com.google.android.exoplayer2.Player.COMMAND_SET_REPEAT_MODE;
import static com.google.android.exoplayer2.Player.COMMAND_SET_SHUFFLE_MODE;
import static com.google.android.exoplayer2.Player.COMMAND_SET_SPEED_AND_PITCH;
import static com.google.android.exoplayer2.Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS;
import static com.google.android.exoplayer2.Player.COMMAND_SET_VIDEO_SURFACE;
import static com.google.android.exoplayer2.Player.COMMAND_SET_VOLUME;
import static com.google.android.exoplayer2.Player.COMMAND_STOP;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_AUTO_TRANSITION;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_INTERNAL;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_REMOVE;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK;
import static com.google.android.exoplayer2.Player.EVENT_MEDIA_METADATA_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_PLAYLIST_METADATA_CHANGED;
import static com.google.android.exoplayer2.Player.EVENT_TRACK_SELECTION_PARAMETERS_CHANGED;
import static com.google.android.exoplayer2.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO;
import static com.google.android.exoplayer2.Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED;
import static com.google.android.exoplayer2.Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT;
import static com.google.android.exoplayer2.Player.MEDIA_ITEM_TRANSITION_REASON_SEEK;
import static com.google.android.exoplayer2.Player.PLAYBACK_SUPPRESSION_REASON_NONE;
import static com.google.android.exoplayer2.Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS;
import static com.google.android.exoplayer2.Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST;
import static com.google.android.exoplayer2.Player.STATE_BUFFERING;
import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.Player.STATE_IDLE;
import static com.google.android.exoplayer2.Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED;
import static com.google.android.exoplayer2.Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE;
import static com.google.android.exoplayer2.Renderer.MSG_SET_AUDIO_ATTRIBUTES;
import static com.google.android.exoplayer2.Renderer.MSG_SET_AUDIO_SESSION_ID;
import static com.google.android.exoplayer2.Renderer.MSG_SET_AUX_EFFECT_INFO;
import static com.google.android.exoplayer2.Renderer.MSG_SET_CAMERA_MOTION_LISTENER;
import static com.google.android.exoplayer2.Renderer.MSG_SET_CHANGE_FRAME_RATE_STRATEGY;
import static com.google.android.exoplayer2.Renderer.MSG_SET_SCALING_MODE;
import static com.google.android.exoplayer2.Renderer.MSG_SET_SKIP_SILENCE_ENABLED;
import static com.google.android.exoplayer2.Renderer.MSG_SET_VIDEO_FRAME_METADATA_LISTENER;
import static com.google.android.exoplayer2.Renderer.MSG_SET_VIDEO_OUTPUT;
import static com.google.android.exoplayer2.Renderer.MSG_SET_VOLUME;
import static com.google.android.exoplayer2.util.Assertions.checkNotNull;
import static com.google.android.exoplayer2.util.Assertions.checkState;
import static com.google.android.exoplayer2.util.Util.castNonNull;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaFormat;
import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import androidx.annotation.DoNotInline;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.google.android.exoplayer2.ExoPlayer.AudioOffloadListener;
import com.google.android.exoplayer2.Player.Commands;
import com.google.android.exoplayer2.Player.DiscontinuityReason;
import com.google.android.exoplayer2.Player.Events;
import com.google.android.exoplayer2.Player.Listener;
import com.google.android.exoplayer2.Player.PlayWhenReadyChangeReason;
import com.google.android.exoplayer2.Player.PlaybackSuppressionReason;
import com.google.android.exoplayer2.Player.PositionInfo;
import com.google.android.exoplayer2.Player.RepeatMode;
import com.google.android.exoplayer2.Player.State;
import com.google.android.exoplayer2.Player.TimelineChangeReason;
import com.google.android.exoplayer2.PlayerMessage.Target;
import com.google.android.exoplayer2.Renderer.MessageType;
import com.google.android.exoplayer2.analytics.AnalyticsCollector;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.analytics.PlayerId;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.AuxEffectInfo;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;
import com.google.android.exoplayer2.source.ShuffleOrder;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectorResult;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.ConditionVariable;
import com.google.android.exoplayer2.util.HandlerWrapper;
import com.google.android.exoplayer2.util.ListenerSet;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.PriorityTaskManager;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoDecoderOutputBufferRenderer;
import com.google.android.exoplayer2.video.VideoFrameMetadataListener;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.google.android.exoplayer2.video.VideoSize;
import com.google.android.exoplayer2.video.spherical.CameraMotionListener;
import com.google.android.exoplayer2.video.spherical.SphericalGLSurfaceView;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeoutException;

/** A helper class for the {@link SimpleExoPlayer} implementation of {@link ExoPlayer}. */
/* package */ final class ExoPlayerImpl {

  static {
    ExoPlayerLibraryInfo.registerModule("goog.exo.exoplayer");
  }

  private static final String TAG = "ExoPlayerImpl";

  /**
   * This empty track selector result can only be used for {@link PlaybackInfo#trackSelectorResult}
   * when the player does not have any track selection made (such as when player is reset, or when
   * player seeks to an unprepared period). It will not be used as result of any {@link
   * TrackSelector#selectTracks(RendererCapabilities[], TrackGroupArray, MediaPeriodId, Timeline)}
   * operation.
   */
  /* package */ final TrackSelectorResult emptyTrackSelectorResult;
  /* package */ final Commands permanentAvailableCommands;

  private final ConditionVariable constructorFinished;
  private final Context applicationContext;
  private final Player wrappingPlayer;
  private final Renderer[] renderers;
  private final TrackSelector trackSelector;
  private final HandlerWrapper playbackInfoUpdateHandler;
  private final ExoPlayerImplInternal.PlaybackInfoUpdateListener playbackInfoUpdateListener;
  private final ExoPlayerImplInternal internalPlayer;

  private final ListenerSet<Listener> listeners;
  // TODO(b/187152483): Remove this once all events are dispatched via ListenerSet.
  private final CopyOnWriteArraySet<Listener> listenerArraySet;
  private final CopyOnWriteArraySet<AudioOffloadListener> audioOffloadListeners;
  private final Timeline.Period period;
  private final Timeline.Window window;
  private final List<MediaSourceHolderSnapshot> mediaSourceHolderSnapshots;
  private final boolean useLazyPreparation;
  private final MediaSource.Factory mediaSourceFactory;
  private final AnalyticsCollector analyticsCollector;
  private final Looper applicationLooper;
  private final BandwidthMeter bandwidthMeter;
  private final long seekBackIncrementMs;
  private final long seekForwardIncrementMs;
  private final Clock clock;
  private final ComponentListener componentListener;
  private final FrameMetadataListener frameMetadataListener;
  private final AudioBecomingNoisyManager audioBecomingNoisyManager;
  private final AudioFocusManager audioFocusManager;
  private final StreamVolumeManager streamVolumeManager;
  private final WakeLockManager wakeLockManager;
  private final WifiLockManager wifiLockManager;
  private final long detachSurfaceTimeoutMs;

  private @RepeatMode int repeatMode;
  private boolean shuffleModeEnabled;
  private int pendingOperationAcks;
  private @DiscontinuityReason int pendingDiscontinuityReason;
  private boolean pendingDiscontinuity;
  private @PlayWhenReadyChangeReason int pendingPlayWhenReadyChangeReason;
  private boolean foregroundMode;
  private SeekParameters seekParameters;
  private ShuffleOrder shuffleOrder;
  private boolean pauseAtEndOfMediaItems;
  private Commands availableCommands;
  private MediaMetadata mediaMetadata;
  private MediaMetadata playlistMetadata;
  @Nullable private Format videoFormat;
  @Nullable private Format audioFormat;
  @Nullable private AudioTrack keepSessionIdAudioTrack;
  @Nullable private Object videoOutput;
  @Nullable private Surface ownedSurface;
  @Nullable private SurfaceHolder surfaceHolder;
  @Nullable private SphericalGLSurfaceView sphericalGLSurfaceView;
  private boolean surfaceHolderSurfaceIsVideoOutput;
  @Nullable private TextureView textureView;
  private @C.VideoScalingMode int videoScalingMode;
  private @C.VideoChangeFrameRateStrategy int videoChangeFrameRateStrategy;
  private int surfaceWidth;
  private int surfaceHeight;
  @Nullable private DecoderCounters videoDecoderCounters;
  @Nullable private DecoderCounters audioDecoderCounters;
  private int audioSessionId;
  private AudioAttributes audioAttributes;
  private float volume;
  private boolean skipSilenceEnabled;
  private List<Cue> currentCues;
  @Nullable private VideoFrameMetadataListener videoFrameMetadataListener;
  @Nullable private CameraMotionListener cameraMotionListener;
  private boolean throwsWhenUsingWrongThread;
  private boolean hasNotifiedFullWrongThreadWarning;
  @Nullable private PriorityTaskManager priorityTaskManager;
  private boolean isPriorityTaskManagerRegistered;
  private boolean playerReleased;
  private DeviceInfo deviceInfo;
  private VideoSize videoSize;

  // MediaMetadata built from static (TrackGroup Format) and dynamic (onMetadata(Metadata)) metadata
  // sources.
  private MediaMetadata staticAndDynamicMediaMetadata;

  // Playback information when there is no pending seek/set source operation.
  private PlaybackInfo playbackInfo;

  // Playback information when there is a pending seek/set source operation.
  private int maskingWindowIndex;
  private int maskingPeriodIndex;
  private long maskingWindowPositionMs;

  @SuppressLint("HandlerLeak")
  public ExoPlayerImpl(ExoPlayer.Builder builder, Player wrappingPlayer) {
    constructorFinished = new ConditionVariable();
    try {
      Log.i(
          TAG,
          "Init "
              + Integer.toHexString(System.identityHashCode(this))
              + " ["
              + ExoPlayerLibraryInfo.VERSION_SLASHY
              + "] ["
              + Util.DEVICE_DEBUG_INFO
              + "]");
      applicationContext = builder.context.getApplicationContext();
      analyticsCollector = builder.analyticsCollectorSupplier.get();
      priorityTaskManager = builder.priorityTaskManager;
      audioAttributes = builder.audioAttributes;
      videoScalingMode = builder.videoScalingMode;
      videoChangeFrameRateStrategy = builder.videoChangeFrameRateStrategy;
      skipSilenceEnabled = builder.skipSilenceEnabled;
      detachSurfaceTimeoutMs = builder.detachSurfaceTimeoutMs;
      componentListener = new ComponentListener();
      frameMetadataListener = new FrameMetadataListener();
      Handler eventHandler = new Handler(builder.looper);
      renderers =
          builder
              .renderersFactorySupplier
              .get()
              .createRenderers(
                  eventHandler,
                  componentListener,
                  componentListener,
                  componentListener,
                  componentListener);
      checkState(renderers.length > 0);
      this.trackSelector = builder.trackSelectorSupplier.get();
      this.mediaSourceFactory = builder.mediaSourceFactorySupplier.get();
      this.bandwidthMeter = builder.bandwidthMeterSupplier.get();
      this.useLazyPreparation = builder.useLazyPreparation;
      this.seekParameters = builder.seekParameters;
      this.seekBackIncrementMs = builder.seekBackIncrementMs;
      this.seekForwardIncrementMs = builder.seekForwardIncrementMs;
      this.pauseAtEndOfMediaItems = builder.pauseAtEndOfMediaItems;
      this.applicationLooper = builder.looper;
      this.clock = builder.clock;
      this.wrappingPlayer = wrappingPlayer;
      listeners =
          new ListenerSet<>(
              applicationLooper,
              clock,
              (listener, flags) -> listener.onEvents(wrappingPlayer, new Events(flags)));
      listenerArraySet = new CopyOnWriteArraySet<>();
      audioOffloadListeners = new CopyOnWriteArraySet<>();
      mediaSourceHolderSnapshots = new ArrayList<>();
      shuffleOrder = new ShuffleOrder.DefaultShuffleOrder(/* length= */ 0);
      emptyTrackSelectorResult =
          new TrackSelectorResult(
              new RendererConfiguration[renderers.length],
              new ExoTrackSelection[renderers.length],
              TracksInfo.EMPTY,
              /* info= */ null);
      period = new Timeline.Period();
      window = new Timeline.Window();
      permanentAvailableCommands =
          new Commands.Builder()
              .addAll(
                  COMMAND_PLAY_PAUSE,
                  COMMAND_PREPARE,
                  COMMAND_STOP,
                  COMMAND_SET_SPEED_AND_PITCH,
                  COMMAND_SET_SHUFFLE_MODE,
                  COMMAND_SET_REPEAT_MODE,
                  COMMAND_GET_CURRENT_MEDIA_ITEM,
                  COMMAND_GET_TIMELINE,
                  COMMAND_GET_MEDIA_ITEMS_METADATA,
                  COMMAND_SET_MEDIA_ITEMS_METADATA,
                  COMMAND_CHANGE_MEDIA_ITEMS,
                  COMMAND_GET_TRACK_INFOS,
                  COMMAND_GET_AUDIO_ATTRIBUTES,
                  COMMAND_GET_VOLUME,
                  COMMAND_GET_DEVICE_VOLUME,
                  COMMAND_SET_VOLUME,
                  COMMAND_SET_DEVICE_VOLUME,
                  COMMAND_ADJUST_DEVICE_VOLUME,
                  COMMAND_SET_VIDEO_SURFACE,
                  COMMAND_GET_TEXT)
              .addIf(
                  COMMAND_SET_TRACK_SELECTION_PARAMETERS, trackSelector.isSetParametersSupported())
              .build();
      availableCommands =
          new Commands.Builder()
              .addAll(permanentAvailableCommands)
              .add(COMMAND_SEEK_TO_DEFAULT_POSITION)
              .add(COMMAND_SEEK_TO_MEDIA_ITEM)
              .build();
      playbackInfoUpdateHandler = clock.createHandler(applicationLooper, /* callback= */ null);
      playbackInfoUpdateListener =
          playbackInfoUpdate ->
              playbackInfoUpdateHandler.post(() -> handlePlaybackInfo(playbackInfoUpdate));
      playbackInfo = PlaybackInfo.createDummy(emptyTrackSelectorResult);
      analyticsCollector.setPlayer(wrappingPlayer, applicationLooper);
      PlayerId playerId = Util.SDK_INT < 31 ? new PlayerId() : Api31.createPlayerId();
      internalPlayer =
          new ExoPlayerImplInternal(
              renderers,
              trackSelector,
              emptyTrackSelectorResult,
              builder.loadControlSupplier.get(),
              bandwidthMeter,
              repeatMode,
              shuffleModeEnabled,
              analyticsCollector,
              seekParameters,
              builder.livePlaybackSpeedControl,
              builder.releaseTimeoutMs,
              pauseAtEndOfMediaItems,
              applicationLooper,
              clock,
              playbackInfoUpdateListener,
              playerId);

      volume = 1;
      repeatMode = Player.REPEAT_MODE_OFF;
      mediaMetadata = MediaMetadata.EMPTY;
      playlistMetadata = MediaMetadata.EMPTY;
      staticAndDynamicMediaMetadata = MediaMetadata.EMPTY;
      maskingWindowIndex = C.INDEX_UNSET;
      if (Util.SDK_INT < 21) {
        audioSessionId = initializeKeepSessionIdAudioTrack(C.AUDIO_SESSION_ID_UNSET);
      } else {
        audioSessionId = Util.generateAudioSessionIdV21(applicationContext);
      }
      currentCues = ImmutableList.of();
      throwsWhenUsingWrongThread = true;

      addListener(analyticsCollector);
      bandwidthMeter.addEventListener(new Handler(applicationLooper), analyticsCollector);
      addAudioOffloadListener(componentListener);
      if (builder.foregroundModeTimeoutMs > 0) {
        experimentalSetForegroundModeTimeoutMs(builder.foregroundModeTimeoutMs);
      }

      audioBecomingNoisyManager =
          new AudioBecomingNoisyManager(builder.context, eventHandler, componentListener);
      audioBecomingNoisyManager.setEnabled(builder.handleAudioBecomingNoisy);
      audioFocusManager = new AudioFocusManager(builder.context, eventHandler, componentListener);
      audioFocusManager.setAudioAttributes(builder.handleAudioFocus ? audioAttributes : null);
      streamVolumeManager =
          new StreamVolumeManager(builder.context, eventHandler, componentListener);
      streamVolumeManager.setStreamType(Util.getStreamTypeForAudioUsage(audioAttributes.usage));
      wakeLockManager = new WakeLockManager(builder.context);
      wakeLockManager.setEnabled(builder.wakeMode != C.WAKE_MODE_NONE);
      wifiLockManager = new WifiLockManager(builder.context);
      wifiLockManager.setEnabled(builder.wakeMode == C.WAKE_MODE_NETWORK);
      deviceInfo = createDeviceInfo(streamVolumeManager);
      videoSize = VideoSize.UNKNOWN;

      sendRendererMessage(TRACK_TYPE_AUDIO, MSG_SET_AUDIO_SESSION_ID, audioSessionId);
      sendRendererMessage(TRACK_TYPE_VIDEO, MSG_SET_AUDIO_SESSION_ID, audioSessionId);
      sendRendererMessage(TRACK_TYPE_AUDIO, MSG_SET_AUDIO_ATTRIBUTES, audioAttributes);
      sendRendererMessage(TRACK_TYPE_VIDEO, MSG_SET_SCALING_MODE, videoScalingMode);
      sendRendererMessage(
          TRACK_TYPE_VIDEO, MSG_SET_CHANGE_FRAME_RATE_STRATEGY, videoChangeFrameRateStrategy);
      sendRendererMessage(TRACK_TYPE_AUDIO, MSG_SET_SKIP_SILENCE_ENABLED, skipSilenceEnabled);
      sendRendererMessage(
          TRACK_TYPE_VIDEO, MSG_SET_VIDEO_FRAME_METADATA_LISTENER, frameMetadataListener);
      sendRendererMessage(
          TRACK_TYPE_CAMERA_MOTION, MSG_SET_CAMERA_MOTION_LISTENER, frameMetadataListener);
    } finally {
      constructorFinished.open();
    }
  }

  /**
   * Sets a limit on the time a call to {@link #setForegroundMode} can spend. If a call to {@link
   * #setForegroundMode} takes more than {@code timeoutMs} milliseconds to complete, the player will
   * raise an error via {@link Player.Listener#onPlayerError}.
   *
   * <p>This method is experimental, and will be renamed or removed in a future release. It should
   * only be called before the player is used.
   *
   * @param timeoutMs The time limit in milliseconds.
   */
  public void experimentalSetForegroundModeTimeoutMs(long timeoutMs) {
    internalPlayer.experimentalSetForegroundModeTimeoutMs(timeoutMs);
  }

  public void experimentalSetOffloadSchedulingEnabled(boolean offloadSchedulingEnabled) {
    verifyApplicationThread();
    internalPlayer.experimentalSetOffloadSchedulingEnabled(offloadSchedulingEnabled);
  }

  public boolean experimentalIsSleepingForOffload() {
    verifyApplicationThread();
    return playbackInfo.sleepingForOffload;
  }

  public Looper getPlaybackLooper() {
    // Don't verify application thread. We allow calls to this method from any thread.
    return internalPlayer.getPlaybackLooper();
  }

  public Looper getApplicationLooper() {
    // Don't verify application thread. We allow calls to this method from any thread.
    return applicationLooper;
  }

  public Clock getClock() {
    // Don't verify application thread. We allow calls to this method from any thread.
    return clock;
  }

  public void addAudioOffloadListener(AudioOffloadListener listener) {
    // Don't verify application thread. We allow calls to this method from any thread.
    audioOffloadListeners.add(listener);
  }

  public void removeAudioOffloadListener(AudioOffloadListener listener) {
    // Don't verify application thread. We allow calls to this method from any thread.
    audioOffloadListeners.remove(listener);
  }

  public Commands getAvailableCommands() {
    verifyApplicationThread();
    return availableCommands;
  }

  public @State int getPlaybackState() {
    verifyApplicationThread();
    return playbackInfo.playbackState;
  }

  public @PlaybackSuppressionReason int getPlaybackSuppressionReason() {
    verifyApplicationThread();
    return playbackInfo.playbackSuppressionReason;
  }

  @Nullable
  public ExoPlaybackException getPlayerError() {
    verifyApplicationThread();
    return playbackInfo.playbackError;
  }

  /** @deprecated Use {@link #prepare()} instead. */
  @Deprecated
  public void retry() {
    prepare();
  }

  public void prepare() {
    verifyApplicationThread();
    boolean playWhenReady = getPlayWhenReady();
    @AudioFocusManager.PlayerCommand
    int playerCommand = audioFocusManager.updateAudioFocus(playWhenReady, Player.STATE_BUFFERING);
    updatePlayWhenReady(
        playWhenReady, playerCommand, getPlayWhenReadyChangeReason(playWhenReady, playerCommand));
    if (playbackInfo.playbackState != Player.STATE_IDLE) {
      return;
    }
    PlaybackInfo playbackInfo = this.playbackInfo.copyWithPlaybackError(null);
    playbackInfo =
        playbackInfo.copyWithPlaybackState(
            playbackInfo.timeline.isEmpty() ? STATE_ENDED : STATE_BUFFERING);
    // Trigger internal prepare first before updating the playback info and notifying external
    // listeners to ensure that new operations issued in the listener notifications reach the
    // player after this prepare. The internal player can't change the playback info immediately
    // because it uses a callback.
    pendingOperationAcks++;
    internalPlayer.prepare();
    updatePlaybackInfo(
        playbackInfo,
        /* ignored */ TIMELINE_CHANGE_REASON_SOURCE_UPDATE,
        /* ignored */ PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        /* seekProcessed= */ false,
        /* positionDiscontinuity= */ false,
        /* ignored */ DISCONTINUITY_REASON_INTERNAL,
        /* ignored */ C.TIME_UNSET,
        /* ignored */ C.INDEX_UNSET);
  }

  /**
   * @deprecated Use {@link #setMediaSource(MediaSource)} and {@link ExoPlayer#prepare()} instead.
   */
  @Deprecated
  public void prepare(MediaSource mediaSource) {
    verifyApplicationThread();
    setMediaSource(mediaSource);
    prepare();
  }

  /**
   * @deprecated Use {@link #setMediaSource(MediaSource, boolean)} and {@link ExoPlayer#prepare()}
   *     instead.
   */
  @Deprecated
  public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {
    verifyApplicationThread();
    setMediaSource(mediaSource, resetPosition);
    prepare();
  }

  public void setMediaItems(List<MediaItem> mediaItems, boolean resetPosition) {
    verifyApplicationThread();
    setMediaSources(createMediaSources(mediaItems), resetPosition);
  }

  public void setMediaItems(List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
    verifyApplicationThread();
    setMediaSources(createMediaSources(mediaItems), startIndex, startPositionMs);
  }

  public void setMediaSource(MediaSource mediaSource) {
    verifyApplicationThread();
    setMediaSources(Collections.singletonList(mediaSource));
  }

  public void setMediaSource(MediaSource mediaSource, long startPositionMs) {
    verifyApplicationThread();
    setMediaSources(
        Collections.singletonList(mediaSource), /* startWindowIndex= */ 0, startPositionMs);
  }

  public void setMediaSource(MediaSource mediaSource, boolean resetPosition) {
    verifyApplicationThread();
    setMediaSources(Collections.singletonList(mediaSource), resetPosition);
  }

  public void setMediaSources(List<MediaSource> mediaSources) {
    verifyApplicationThread();
    setMediaSources(mediaSources, /* resetPosition= */ true);
  }

  public void setMediaSources(List<MediaSource> mediaSources, boolean resetPosition) {
    verifyApplicationThread();
    setMediaSourcesInternal(
        mediaSources,
        /* startWindowIndex= */ C.INDEX_UNSET,
        /* startPositionMs= */ C.TIME_UNSET,
        /* resetToDefaultPosition= */ resetPosition);
  }

  public void setMediaSources(
      List<MediaSource> mediaSources, int startWindowIndex, long startPositionMs) {
    verifyApplicationThread();
    setMediaSourcesInternal(
        mediaSources, startWindowIndex, startPositionMs, /* resetToDefaultPosition= */ false);
  }

  public void addMediaItems(int index, List<MediaItem> mediaItems) {
    verifyApplicationThread();
    index = min(index, mediaSourceHolderSnapshots.size());
    addMediaSources(index, createMediaSources(mediaItems));
  }

  public void addMediaSource(MediaSource mediaSource) {
    verifyApplicationThread();
    addMediaSources(Collections.singletonList(mediaSource));
  }

  public void addMediaSource(int index, MediaSource mediaSource) {
    verifyApplicationThread();
    addMediaSources(index, Collections.singletonList(mediaSource));
  }

  public void addMediaSources(List<MediaSource> mediaSources) {
    verifyApplicationThread();
    addMediaSources(/* index= */ mediaSourceHolderSnapshots.size(), mediaSources);
  }

  public void addMediaSources(int index, List<MediaSource> mediaSources) {
    verifyApplicationThread();
    Assertions.checkArgument(index >= 0);
    Timeline oldTimeline = getCurrentTimeline();
    pendingOperationAcks++;
    List<MediaSourceList.MediaSourceHolder> holders = addMediaSourceHolders(index, mediaSources);
    Timeline newTimeline = createMaskingTimeline();
    PlaybackInfo newPlaybackInfo =
        maskTimelineAndPosition(
            playbackInfo,
            newTimeline,
            getPeriodPositionUsAfterTimelineChanged(oldTimeline, newTimeline));
    internalPlayer.addMediaSources(index, holders, shuffleOrder);
    updatePlaybackInfo(
        newPlaybackInfo,
        /* timelineChangeReason= */ TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
        /* ignored */ PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        /* seekProcessed= */ false,
        /* positionDiscontinuity= */ false,
        /* ignored */ DISCONTINUITY_REASON_INTERNAL,
        /* ignored */ C.TIME_UNSET,
        /* ignored */ C.INDEX_UNSET);
  }

  public void removeMediaItems(int fromIndex, int toIndex) {
    verifyApplicationThread();
    toIndex = min(toIndex, mediaSourceHolderSnapshots.size());
    PlaybackInfo newPlaybackInfo = removeMediaItemsInternal(fromIndex, toIndex);
    boolean positionDiscontinuity =
        !newPlaybackInfo.periodId.periodUid.equals(playbackInfo.periodId.periodUid);
    updatePlaybackInfo(
        newPlaybackInfo,
        /* timelineChangeReason= */ TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
        /* ignored */ PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        /* seekProcessed= */ false,
        positionDiscontinuity,
        DISCONTINUITY_REASON_REMOVE,
        /* discontinuityWindowStartPositionUs= */ getCurrentPositionUsInternal(newPlaybackInfo),
        /* ignored */ C.INDEX_UNSET);
  }

  public void moveMediaItems(int fromIndex, int toIndex, int newFromIndex) {
    verifyApplicationThread();
    Assertions.checkArgument(
        fromIndex >= 0
            && fromIndex <= toIndex
            && toIndex <= mediaSourceHolderSnapshots.size()
            && newFromIndex >= 0);
    Timeline oldTimeline = getCurrentTimeline();
    pendingOperationAcks++;
    newFromIndex = min(newFromIndex, mediaSourceHolderSnapshots.size() - (toIndex - fromIndex));
    Util.moveItems(mediaSourceHolderSnapshots, fromIndex, toIndex, newFromIndex);
    Timeline newTimeline = createMaskingTimeline();
    PlaybackInfo newPlaybackInfo =
        maskTimelineAndPosition(
            playbackInfo,
            newTimeline,
            getPeriodPositionUsAfterTimelineChanged(oldTimeline, newTimeline));
    internalPlayer.moveMediaSources(fromIndex, toIndex, newFromIndex, shuffleOrder);
    updatePlaybackInfo(
        newPlaybackInfo,
        /* timelineChangeReason= */ TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
        /* ignored */ PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        /* seekProcessed= */ false,
        /* positionDiscontinuity= */ false,
        /* ignored */ DISCONTINUITY_REASON_INTERNAL,
        /* ignored */ C.TIME_UNSET,
        /* ignored */ C.INDEX_UNSET);
  }

  public void setShuffleOrder(ShuffleOrder shuffleOrder) {
    verifyApplicationThread();
    Timeline timeline = createMaskingTimeline();
    PlaybackInfo newPlaybackInfo =
        maskTimelineAndPosition(
            playbackInfo,
            timeline,
            maskWindowPositionMsOrGetPeriodPositionUs(
                timeline, getCurrentMediaItemIndex(), getCurrentPosition()));
    pendingOperationAcks++;
    this.shuffleOrder = shuffleOrder;
    internalPlayer.setShuffleOrder(shuffleOrder);
    updatePlaybackInfo(
        newPlaybackInfo,
        /* timelineChangeReason= */ TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
        /* ignored */ PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        /* seekProcessed= */ false,
        /* positionDiscontinuity= */ false,
        /* ignored */ DISCONTINUITY_REASON_INTERNAL,
        /* ignored */ C.TIME_UNSET,
        /* ignored */ C.INDEX_UNSET);
  }

  public void setPauseAtEndOfMediaItems(boolean pauseAtEndOfMediaItems) {
    verifyApplicationThread();
    if (this.pauseAtEndOfMediaItems == pauseAtEndOfMediaItems) {
      return;
    }
    this.pauseAtEndOfMediaItems = pauseAtEndOfMediaItems;
    internalPlayer.setPauseAtEndOfWindow(pauseAtEndOfMediaItems);
  }

  public boolean getPauseAtEndOfMediaItems() {
    verifyApplicationThread();
    return pauseAtEndOfMediaItems;
  }

  public void setPlayWhenReady(boolean playWhenReady) {
    verifyApplicationThread();
    @AudioFocusManager.PlayerCommand
    int playerCommand = audioFocusManager.updateAudioFocus(playWhenReady, getPlaybackState());
    updatePlayWhenReady(
        playWhenReady, playerCommand, getPlayWhenReadyChangeReason(playWhenReady, playerCommand));
  }

  public boolean getPlayWhenReady() {
    verifyApplicationThread();
    return playbackInfo.playWhenReady;
  }

  public void setRepeatMode(@RepeatMode int repeatMode) {
    verifyApplicationThread();
    if (this.repeatMode != repeatMode) {
      this.repeatMode = repeatMode;
      internalPlayer.setRepeatMode(repeatMode);
      listeners.queueEvent(
          Player.EVENT_REPEAT_MODE_CHANGED, listener -> listener.onRepeatModeChanged(repeatMode));
      updateAvailableCommands();
      listeners.flushEvents();
    }
  }

  public @RepeatMode int getRepeatMode() {
    verifyApplicationThread();
    return repeatMode;
  }

  public void setShuffleModeEnabled(boolean shuffleModeEnabled) {
    verifyApplicationThread();
    if (this.shuffleModeEnabled != shuffleModeEnabled) {
      this.shuffleModeEnabled = shuffleModeEnabled;
      internalPlayer.setShuffleModeEnabled(shuffleModeEnabled);
      listeners.queueEvent(
          Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
          listener -> listener.onShuffleModeEnabledChanged(shuffleModeEnabled));
      updateAvailableCommands();
      listeners.flushEvents();
    }
  }

  public boolean getShuffleModeEnabled() {
    verifyApplicationThread();
    return shuffleModeEnabled;
  }

  public boolean isLoading() {
    verifyApplicationThread();
    return playbackInfo.isLoading;
  }

  public void seekTo(int mediaItemIndex, long positionMs) {
    verifyApplicationThread();
    analyticsCollector.notifySeekStarted();
    Timeline timeline = playbackInfo.timeline;
    if (mediaItemIndex < 0
        || (!timeline.isEmpty() && mediaItemIndex >= timeline.getWindowCount())) {
      throw new IllegalSeekPositionException(timeline, mediaItemIndex, positionMs);
    }
    pendingOperationAcks++;
    if (isPlayingAd()) {
      // TODO: Investigate adding support for seeking during ads. This is complicated to do in
      // general because the midroll ad preceding the seek destination must be played before the
      // content position can be played, if a different ad is playing at the moment.
      Log.w(TAG, "seekTo ignored because an ad is playing");
      ExoPlayerImplInternal.PlaybackInfoUpdate playbackInfoUpdate =
          new ExoPlayerImplInternal.PlaybackInfoUpdate(this.playbackInfo);
      playbackInfoUpdate.incrementPendingOperationAcks(1);
      playbackInfoUpdateListener.onPlaybackInfoUpdate(playbackInfoUpdate);
      return;
    }
    @Player.State
    int newPlaybackState =
        getPlaybackState() == Player.STATE_IDLE ? Player.STATE_IDLE : STATE_BUFFERING;
    int oldMaskingMediaItemIndex = getCurrentMediaItemIndex();
    PlaybackInfo newPlaybackInfo = playbackInfo.copyWithPlaybackState(newPlaybackState);
    newPlaybackInfo =
        maskTimelineAndPosition(
            newPlaybackInfo,
            timeline,
            maskWindowPositionMsOrGetPeriodPositionUs(timeline, mediaItemIndex, positionMs));
    internalPlayer.seekTo(timeline, mediaItemIndex, Util.msToUs(positionMs));
    updatePlaybackInfo(
        newPlaybackInfo,
        /* ignored */ TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
        /* ignored */ PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        /* seekProcessed= */ true,
        /* positionDiscontinuity= */ true,
        /* positionDiscontinuityReason= */ DISCONTINUITY_REASON_SEEK,
        /* discontinuityWindowStartPositionUs= */ getCurrentPositionUsInternal(newPlaybackInfo),
        oldMaskingMediaItemIndex);
  }

  public long getSeekBackIncrement() {
    verifyApplicationThread();
    return seekBackIncrementMs;
  }

  public long getSeekForwardIncrement() {
    verifyApplicationThread();
    return seekForwardIncrementMs;
  }

  public long getMaxSeekToPreviousPosition() {
    verifyApplicationThread();
    return C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS;
  }

  public void setPlaybackParameters(PlaybackParameters playbackParameters) {
    verifyApplicationThread();
    if (playbackParameters == null) {
      playbackParameters = PlaybackParameters.DEFAULT;
    }
    if (playbackInfo.playbackParameters.equals(playbackParameters)) {
      return;
    }
    PlaybackInfo newPlaybackInfo = playbackInfo.copyWithPlaybackParameters(playbackParameters);
    pendingOperationAcks++;
    internalPlayer.setPlaybackParameters(playbackParameters);
    updatePlaybackInfo(
        newPlaybackInfo,
        /* ignored */ TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
        /* ignored */ PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        /* seekProcessed= */ false,
        /* positionDiscontinuity= */ false,
        /* ignored */ DISCONTINUITY_REASON_INTERNAL,
        /* ignored */ C.TIME_UNSET,
        /* ignored */ C.INDEX_UNSET);
  }

  public PlaybackParameters getPlaybackParameters() {
    verifyApplicationThread();
    return playbackInfo.playbackParameters;
  }

  public void setSeekParameters(@Nullable SeekParameters seekParameters) {
    verifyApplicationThread();
    if (seekParameters == null) {
      seekParameters = SeekParameters.DEFAULT;
    }
    if (!this.seekParameters.equals(seekParameters)) {
      this.seekParameters = seekParameters;
      internalPlayer.setSeekParameters(seekParameters);
    }
  }

  public SeekParameters getSeekParameters() {
    verifyApplicationThread();
    return seekParameters;
  }

  public void setForegroundMode(boolean foregroundMode) {
    verifyApplicationThread();
    if (this.foregroundMode != foregroundMode) {
      this.foregroundMode = foregroundMode;
      if (!internalPlayer.setForegroundMode(foregroundMode)) {
        // One of the renderers timed out releasing its resources.
        stop(
            /* reset= */ false,
            ExoPlaybackException.createForUnexpected(
                new ExoTimeoutException(ExoTimeoutException.TIMEOUT_OPERATION_SET_FOREGROUND_MODE),
                PlaybackException.ERROR_CODE_TIMEOUT));
      }
    }
  }

  public void stop() {
    stop(/* reset= */ false);
  }

  public void stop(boolean reset) {
    verifyApplicationThread();
    audioFocusManager.updateAudioFocus(getPlayWhenReady(), Player.STATE_IDLE);
    stop(reset, /* error= */ null);
    currentCues = ImmutableList.of();
  }

  /**
   * Stops the player.
   *
   * @param reset Whether the playlist should be cleared and whether the playback position and
   *     playback error should be reset.
   * @param error An optional {@link ExoPlaybackException} to set.
   */
  public void stop(boolean reset, @Nullable ExoPlaybackException error) {
    PlaybackInfo playbackInfo;
    if (reset) {
      playbackInfo =
          removeMediaItemsInternal(
              /* fromIndex= */ 0, /* toIndex= */ mediaSourceHolderSnapshots.size());
      playbackInfo = playbackInfo.copyWithPlaybackError(null);
    } else {
      playbackInfo = this.playbackInfo.copyWithLoadingMediaPeriodId(this.playbackInfo.periodId);
      playbackInfo.bufferedPositionUs = playbackInfo.positionUs;
      playbackInfo.totalBufferedDurationUs = 0;
    }
    playbackInfo = playbackInfo.copyWithPlaybackState(Player.STATE_IDLE);
    if (error != null) {
      playbackInfo = playbackInfo.copyWithPlaybackError(error);
    }
    pendingOperationAcks++;
    internalPlayer.stop();
    boolean positionDiscontinuity =
        playbackInfo.timeline.isEmpty() && !this.playbackInfo.timeline.isEmpty();
    updatePlaybackInfo(
        playbackInfo,
        TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
        /* ignored */ PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        /* seekProcessed= */ false,
        positionDiscontinuity,
        DISCONTINUITY_REASON_REMOVE,
        /* discontinuityWindowStartPositionUs= */ getCurrentPositionUsInternal(playbackInfo),
        /* ignored */ C.INDEX_UNSET);
  }

  public void release() {
    Log.i(
        TAG,
        "Release "
            + Integer.toHexString(System.identityHashCode(this))
            + " ["
            + ExoPlayerLibraryInfo.VERSION_SLASHY
            + "] ["
            + Util.DEVICE_DEBUG_INFO
            + "] ["
            + ExoPlayerLibraryInfo.registeredModules()
            + "]");
    verifyApplicationThread();
    if (Util.SDK_INT < 21 && keepSessionIdAudioTrack != null) {
      keepSessionIdAudioTrack.release();
      keepSessionIdAudioTrack = null;
    }
    audioBecomingNoisyManager.setEnabled(false);
    streamVolumeManager.release();
    wakeLockManager.setStayAwake(false);
    wifiLockManager.setStayAwake(false);
    audioFocusManager.release();
    if (!internalPlayer.release()) {
      // One of the renderers timed out releasing its resources.
      listeners.sendEvent(
          Player.EVENT_PLAYER_ERROR,
          listener ->
              listener.onPlayerError(
                  ExoPlaybackException.createForUnexpected(
                      new ExoTimeoutException(ExoTimeoutException.TIMEOUT_OPERATION_RELEASE),
                      PlaybackException.ERROR_CODE_TIMEOUT)));
    }
    listeners.release();
    playbackInfoUpdateHandler.removeCallbacksAndMessages(null);
    bandwidthMeter.removeEventListener(analyticsCollector);
    playbackInfo = playbackInfo.copyWithPlaybackState(Player.STATE_IDLE);
    playbackInfo = playbackInfo.copyWithLoadingMediaPeriodId(playbackInfo.periodId);
    playbackInfo.bufferedPositionUs = playbackInfo.positionUs;
    playbackInfo.totalBufferedDurationUs = 0;
    analyticsCollector.release();
    removeSurfaceCallbacks();
    if (ownedSurface != null) {
      ownedSurface.release();
      ownedSurface = null;
    }
    if (isPriorityTaskManagerRegistered) {
      checkNotNull(priorityTaskManager).remove(C.PRIORITY_PLAYBACK);
      isPriorityTaskManagerRegistered = false;
    }
    currentCues = ImmutableList.of();
    playerReleased = true;
  }

  public PlayerMessage createMessage(Target target) {
    verifyApplicationThread();
    return createMessageInternal(target);
  }

  public int getCurrentPeriodIndex() {
    verifyApplicationThread();
    if (playbackInfo.timeline.isEmpty()) {
      return maskingPeriodIndex;
    } else {
      return playbackInfo.timeline.getIndexOfPeriod(playbackInfo.periodId.periodUid);
    }
  }

  public int getCurrentMediaItemIndex() {
    verifyApplicationThread();
    int currentWindowIndex = getCurrentWindowIndexInternal();
    return currentWindowIndex == C.INDEX_UNSET ? 0 : currentWindowIndex;
  }

  public long getDuration() {
    verifyApplicationThread();
    if (isPlayingAd()) {
      MediaPeriodId periodId = playbackInfo.periodId;
      playbackInfo.timeline.getPeriodByUid(periodId.periodUid, period);
      long adDurationUs = period.getAdDurationUs(periodId.adGroupIndex, periodId.adIndexInAdGroup);
      return Util.usToMs(adDurationUs);
    }
    return getContentDuration();
  }

  private long getContentDuration() {
    Timeline timeline = getCurrentTimeline();
    return timeline.isEmpty()
        ? C.TIME_UNSET
        : timeline.getWindow(getCurrentMediaItemIndex(), window).getDurationMs();
  }

  public long getCurrentPosition() {
    verifyApplicationThread();
    return Util.usToMs(getCurrentPositionUsInternal(playbackInfo));
  }

  public long getBufferedPosition() {
    verifyApplicationThread();
    if (isPlayingAd()) {
      return playbackInfo.loadingMediaPeriodId.equals(playbackInfo.periodId)
          ? Util.usToMs(playbackInfo.bufferedPositionUs)
          : getDuration();
    }
    return getContentBufferedPosition();
  }

  public long getTotalBufferedDuration() {
    verifyApplicationThread();
    return Util.usToMs(playbackInfo.totalBufferedDurationUs);
  }

  public boolean isPlayingAd() {
    verifyApplicationThread();
    return playbackInfo.periodId.isAd();
  }

  public int getCurrentAdGroupIndex() {
    verifyApplicationThread();
    return isPlayingAd() ? playbackInfo.periodId.adGroupIndex : C.INDEX_UNSET;
  }

  public int getCurrentAdIndexInAdGroup() {
    verifyApplicationThread();
    return isPlayingAd() ? playbackInfo.periodId.adIndexInAdGroup : C.INDEX_UNSET;
  }

  public long getContentPosition() {
    verifyApplicationThread();
    if (isPlayingAd()) {
      playbackInfo.timeline.getPeriodByUid(playbackInfo.periodId.periodUid, period);
      return playbackInfo.requestedContentPositionUs == C.TIME_UNSET
          ? playbackInfo
              .timeline
              .getWindow(getCurrentMediaItemIndex(), window)
              .getDefaultPositionMs()
          : period.getPositionInWindowMs() + Util.usToMs(playbackInfo.requestedContentPositionUs);
    } else {
      return getCurrentPosition();
    }
  }

  public long getContentBufferedPosition() {
    verifyApplicationThread();
    if (playbackInfo.timeline.isEmpty()) {
      return maskingWindowPositionMs;
    }
    if (playbackInfo.loadingMediaPeriodId.windowSequenceNumber
        != playbackInfo.periodId.windowSequenceNumber) {
      return playbackInfo.timeline.getWindow(getCurrentMediaItemIndex(), window).getDurationMs();
    }
    long contentBufferedPositionUs = playbackInfo.bufferedPositionUs;
    if (playbackInfo.loadingMediaPeriodId.isAd()) {
      Timeline.Period loadingPeriod =
          playbackInfo.timeline.getPeriodByUid(playbackInfo.loadingMediaPeriodId.periodUid, period);
      contentBufferedPositionUs =
          loadingPeriod.getAdGroupTimeUs(playbackInfo.loadingMediaPeriodId.adGroupIndex);
      if (contentBufferedPositionUs == C.TIME_END_OF_SOURCE) {
        contentBufferedPositionUs = loadingPeriod.durationUs;
      }
    }
    return Util.usToMs(
        periodPositionUsToWindowPositionUs(
            playbackInfo.timeline, playbackInfo.loadingMediaPeriodId, contentBufferedPositionUs));
  }

  public int getRendererCount() {
    verifyApplicationThread();
    return renderers.length;
  }

  public @C.TrackType int getRendererType(int index) {
    verifyApplicationThread();
    return renderers[index].getTrackType();
  }

  public Renderer getRenderer(int index) {
    verifyApplicationThread();
    return renderers[index];
  }

  public TrackSelector getTrackSelector() {
    verifyApplicationThread();
    return trackSelector;
  }

  public TrackGroupArray getCurrentTrackGroups() {
    verifyApplicationThread();
    return playbackInfo.trackGroups;
  }

  public TrackSelectionArray getCurrentTrackSelections() {
    verifyApplicationThread();
    return new TrackSelectionArray(playbackInfo.trackSelectorResult.selections);
  }

  public TracksInfo getCurrentTracksInfo() {
    verifyApplicationThread();
    return playbackInfo.trackSelectorResult.tracksInfo;
  }

  public TrackSelectionParameters getTrackSelectionParameters() {
    verifyApplicationThread();
    return trackSelector.getParameters();
  }

  public void setTrackSelectionParameters(TrackSelectionParameters parameters) {
    verifyApplicationThread();
    if (!trackSelector.isSetParametersSupported()
        || parameters.equals(trackSelector.getParameters())) {
      return;
    }
    trackSelector.setParameters(parameters);
    listeners.sendEvent(
        EVENT_TRACK_SELECTION_PARAMETERS_CHANGED,
        listener -> listener.onTrackSelectionParametersChanged(parameters));
  }

  public MediaMetadata getMediaMetadata() {
    verifyApplicationThread();
    return mediaMetadata;
  }

  public MediaMetadata getPlaylistMetadata() {
    verifyApplicationThread();
    return playlistMetadata;
  }

  public void setPlaylistMetadata(MediaMetadata playlistMetadata) {
    verifyApplicationThread();
    checkNotNull(playlistMetadata);
    if (playlistMetadata.equals(this.playlistMetadata)) {
      return;
    }
    this.playlistMetadata = playlistMetadata;
    listeners.sendEvent(
        EVENT_PLAYLIST_METADATA_CHANGED,
        listener -> listener.onPlaylistMetadataChanged(this.playlistMetadata));
  }

  public Timeline getCurrentTimeline() {
    verifyApplicationThread();
    return playbackInfo.timeline;
  }

  public void setVideoScalingMode(@C.VideoScalingMode int videoScalingMode) {
    verifyApplicationThread();
    this.videoScalingMode = videoScalingMode;
    sendRendererMessage(TRACK_TYPE_VIDEO, MSG_SET_SCALING_MODE, videoScalingMode);
  }

  public @C.VideoScalingMode int getVideoScalingMode() {
    return videoScalingMode;
  }

  public void setVideoChangeFrameRateStrategy(
      @C.VideoChangeFrameRateStrategy int videoChangeFrameRateStrategy) {
    verifyApplicationThread();
    if (this.videoChangeFrameRateStrategy == videoChangeFrameRateStrategy) {
      return;
    }
    this.videoChangeFrameRateStrategy = videoChangeFrameRateStrategy;
    sendRendererMessage(
        TRACK_TYPE_VIDEO, MSG_SET_CHANGE_FRAME_RATE_STRATEGY, videoChangeFrameRateStrategy);
  }

  public @C.VideoChangeFrameRateStrategy int getVideoChangeFrameRateStrategy() {
    return videoChangeFrameRateStrategy;
  }

  public VideoSize getVideoSize() {
    return videoSize;
  }

  public void clearVideoSurface() {
    verifyApplicationThread();
    removeSurfaceCallbacks();
    setVideoOutputInternal(/* videoOutput= */ null);
    maybeNotifySurfaceSizeChanged(/* width= */ 0, /* height= */ 0);
  }

  public void clearVideoSurface(@Nullable Surface surface) {
    verifyApplicationThread();
    if (surface != null && surface == videoOutput) {
      clearVideoSurface();
    }
  }

  public void setVideoSurface(@Nullable Surface surface) {
    verifyApplicationThread();
    removeSurfaceCallbacks();
    setVideoOutputInternal(surface);
    int newSurfaceSize = surface == null ? 0 : C.LENGTH_UNSET;
    maybeNotifySurfaceSizeChanged(/* width= */ newSurfaceSize, /* height= */ newSurfaceSize);
  }

  public void setVideoSurfaceHolder(@Nullable SurfaceHolder surfaceHolder) {
    verifyApplicationThread();
    if (surfaceHolder == null) {
      clearVideoSurface();
    } else {
      removeSurfaceCallbacks();
      this.surfaceHolderSurfaceIsVideoOutput = true;
      this.surfaceHolder = surfaceHolder;
      surfaceHolder.addCallback(componentListener);
      Surface surface = surfaceHolder.getSurface();
      if (surface != null && surface.isValid()) {
        setVideoOutputInternal(surface);
        Rect surfaceSize = surfaceHolder.getSurfaceFrame();
        maybeNotifySurfaceSizeChanged(surfaceSize.width(), surfaceSize.height());
      } else {
        setVideoOutputInternal(/* videoOutput= */ null);
        maybeNotifySurfaceSizeChanged(/* width= */ 0, /* height= */ 0);
      }
    }
  }

  public void clearVideoSurfaceHolder(@Nullable SurfaceHolder surfaceHolder) {
    verifyApplicationThread();
    if (surfaceHolder != null && surfaceHolder == this.surfaceHolder) {
      clearVideoSurface();
    }
  }

  public void setVideoSurfaceView(@Nullable SurfaceView surfaceView) {
    verifyApplicationThread();
    if (surfaceView instanceof VideoDecoderOutputBufferRenderer) {
      removeSurfaceCallbacks();
      setVideoOutputInternal(surfaceView);
      setNonVideoOutputSurfaceHolderInternal(surfaceView.getHolder());
    } else if (surfaceView instanceof SphericalGLSurfaceView) {
      removeSurfaceCallbacks();
      sphericalGLSurfaceView = (SphericalGLSurfaceView) surfaceView;
      createMessageInternal(frameMetadataListener)
          .setType(FrameMetadataListener.MSG_SET_SPHERICAL_SURFACE_VIEW)
          .setPayload(sphericalGLSurfaceView)
          .send();
      sphericalGLSurfaceView.addVideoSurfaceListener(componentListener);
      setVideoOutputInternal(sphericalGLSurfaceView.getVideoSurface());
      setNonVideoOutputSurfaceHolderInternal(surfaceView.getHolder());
    } else {
      setVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
    }
  }

  public void clearVideoSurfaceView(@Nullable SurfaceView surfaceView) {
    verifyApplicationThread();
    clearVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
  }

  public void setVideoTextureView(@Nullable TextureView textureView) {
    verifyApplicationThread();
    if (textureView == null) {
      clearVideoSurface();
    } else {
      removeSurfaceCallbacks();
      this.textureView = textureView;
      if (textureView.getSurfaceTextureListener() != null) {
        Log.w(TAG, "Replacing existing SurfaceTextureListener.");
      }
      textureView.setSurfaceTextureListener(componentListener);
      @Nullable
      SurfaceTexture surfaceTexture =
          textureView.isAvailable() ? textureView.getSurfaceTexture() : null;
      if (surfaceTexture == null) {
        setVideoOutputInternal(/* videoOutput= */ null);
        maybeNotifySurfaceSizeChanged(/* width= */ 0, /* height= */ 0);
      } else {
        setSurfaceTextureInternal(surfaceTexture);
        maybeNotifySurfaceSizeChanged(textureView.getWidth(), textureView.getHeight());
      }
    }
  }

  public void clearVideoTextureView(@Nullable TextureView textureView) {
    verifyApplicationThread();
    if (textureView != null && textureView == this.textureView) {
      clearVideoSurface();
    }
  }

  public void setAudioAttributes(AudioAttributes audioAttributes, boolean handleAudioFocus) {
    verifyApplicationThread();
    if (playerReleased) {
      return;
    }
    if (!Util.areEqual(this.audioAttributes, audioAttributes)) {
      this.audioAttributes = audioAttributes;
      sendRendererMessage(TRACK_TYPE_AUDIO, MSG_SET_AUDIO_ATTRIBUTES, audioAttributes);
      streamVolumeManager.setStreamType(Util.getStreamTypeForAudioUsage(audioAttributes.usage));
      // TODO(internal b/187152483): Events should be dispatched via ListenerSet
      for (Listener listener : listenerArraySet) {
        listener.onAudioAttributesChanged(audioAttributes);
      }
    }

    audioFocusManager.setAudioAttributes(handleAudioFocus ? audioAttributes : null);
    boolean playWhenReady = getPlayWhenReady();
    @AudioFocusManager.PlayerCommand
    int playerCommand = audioFocusManager.updateAudioFocus(playWhenReady, getPlaybackState());
    updatePlayWhenReady(
        playWhenReady, playerCommand, getPlayWhenReadyChangeReason(playWhenReady, playerCommand));
  }

  public AudioAttributes getAudioAttributes() {
    return audioAttributes;
  }

  public void setAudioSessionId(int audioSessionId) {
    verifyApplicationThread();
    if (this.audioSessionId == audioSessionId) {
      return;
    }
    if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
      if (Util.SDK_INT < 21) {
        audioSessionId = initializeKeepSessionIdAudioTrack(C.AUDIO_SESSION_ID_UNSET);
      } else {
        audioSessionId = Util.generateAudioSessionIdV21(applicationContext);
      }
    } else if (Util.SDK_INT < 21) {
      // We need to re-initialize keepSessionIdAudioTrack to make sure the session is kept alive for
      // as long as the player is using it.
      initializeKeepSessionIdAudioTrack(audioSessionId);
    }
    this.audioSessionId = audioSessionId;
    sendRendererMessage(TRACK_TYPE_AUDIO, MSG_SET_AUDIO_SESSION_ID, audioSessionId);
    sendRendererMessage(TRACK_TYPE_VIDEO, MSG_SET_AUDIO_SESSION_ID, audioSessionId);
    // TODO(internal b/187152483): Events should be dispatched via ListenerSet
    for (Listener listener : listenerArraySet) {
      listener.onAudioSessionIdChanged(audioSessionId);
    }
  }

  public int getAudioSessionId() {
    return audioSessionId;
  }

  public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) {
    verifyApplicationThread();
    sendRendererMessage(TRACK_TYPE_AUDIO, MSG_SET_AUX_EFFECT_INFO, auxEffectInfo);
  }

  public void clearAuxEffectInfo() {
    setAuxEffectInfo(new AuxEffectInfo(AuxEffectInfo.NO_AUX_EFFECT_ID, /* sendLevel= */ 0f));
  }

  public void setVolume(float volume) {
    verifyApplicationThread();
    volume = Util.constrainValue(volume, /* min= */ 0, /* max= */ 1);
    if (this.volume == volume) {
      return;
    }
    this.volume = volume;
    sendVolumeToRenderers();
    // TODO(internal b/187152483): Events should be dispatched via ListenerSet
    for (Listener listener : listenerArraySet) {
      listener.onVolumeChanged(volume);
    }
  }

  public float getVolume() {
    return volume;
  }

  public boolean getSkipSilenceEnabled() {
    return skipSilenceEnabled;
  }

  public void setSkipSilenceEnabled(boolean skipSilenceEnabled) {
    verifyApplicationThread();
    if (this.skipSilenceEnabled == skipSilenceEnabled) {
      return;
    }
    this.skipSilenceEnabled = skipSilenceEnabled;
    sendRendererMessage(TRACK_TYPE_AUDIO, MSG_SET_SKIP_SILENCE_ENABLED, skipSilenceEnabled);
    notifySkipSilenceEnabledChanged();
  }

  public AnalyticsCollector getAnalyticsCollector() {
    return analyticsCollector;
  }

  public void addAnalyticsListener(AnalyticsListener listener) {
    // Don't verify application thread. We allow calls to this method from any thread.
    checkNotNull(listener);
    analyticsCollector.addListener(listener);
  }

  public void removeAnalyticsListener(AnalyticsListener listener) {
    // Don't verify application thread. We allow calls to this method from any thread.
    analyticsCollector.removeListener(listener);
  }

  public void setHandleAudioBecomingNoisy(boolean handleAudioBecomingNoisy) {
    verifyApplicationThread();
    if (playerReleased) {
      return;
    }
    audioBecomingNoisyManager.setEnabled(handleAudioBecomingNoisy);
  }

  public void setPriorityTaskManager(@Nullable PriorityTaskManager priorityTaskManager) {
    verifyApplicationThread();
    if (Util.areEqual(this.priorityTaskManager, priorityTaskManager)) {
      return;
    }
    if (isPriorityTaskManagerRegistered) {
      checkNotNull(this.priorityTaskManager).remove(C.PRIORITY_PLAYBACK);
    }
    if (priorityTaskManager != null && isLoading()) {
      priorityTaskManager.add(C.PRIORITY_PLAYBACK);
      isPriorityTaskManagerRegistered = true;
    } else {
      isPriorityTaskManagerRegistered = false;
    }
    this.priorityTaskManager = priorityTaskManager;
  }

  @Nullable
  public Format getVideoFormat() {
    return videoFormat;
  }

  @Nullable
  public Format getAudioFormat() {
    return audioFormat;
  }

  @Nullable
  public DecoderCounters getVideoDecoderCounters() {
    return videoDecoderCounters;
  }

  @Nullable
  public DecoderCounters getAudioDecoderCounters() {
    return audioDecoderCounters;
  }

  public void setVideoFrameMetadataListener(VideoFrameMetadataListener listener) {
    verifyApplicationThread();
    videoFrameMetadataListener = listener;
    createMessageInternal(frameMetadataListener)
        .setType(FrameMetadataListener.MSG_SET_VIDEO_FRAME_METADATA_LISTENER)
        .setPayload(listener)
        .send();
  }

  public void clearVideoFrameMetadataListener(VideoFrameMetadataListener listener) {
    verifyApplicationThread();
    if (videoFrameMetadataListener != listener) {
      return;
    }
    createMessageInternal(frameMetadataListener)
        .setType(FrameMetadataListener.MSG_SET_VIDEO_FRAME_METADATA_LISTENER)
        .setPayload(null)
        .send();
  }

  public void setCameraMotionListener(CameraMotionListener listener) {
    verifyApplicationThread();
    cameraMotionListener = listener;
    createMessageInternal(frameMetadataListener)
        .setType(FrameMetadataListener.MSG_SET_CAMERA_MOTION_LISTENER)
        .setPayload(listener)
        .send();
  }

  public void clearCameraMotionListener(CameraMotionListener listener) {
    verifyApplicationThread();
    if (cameraMotionListener != listener) {
      return;
    }
    createMessageInternal(frameMetadataListener)
        .setType(FrameMetadataListener.MSG_SET_CAMERA_MOTION_LISTENER)
        .setPayload(null)
        .send();
  }

  public List<Cue> getCurrentCues() {
    verifyApplicationThread();
    return currentCues;
  }

  public void addListener(Listener listener) {
    // Don't verify application thread. We allow calls to this method from any thread.
    checkNotNull(listener);
    listeners.add(listener);
    listenerArraySet.add(listener);
  }

  public void removeListener(Listener listener) {
    // Don't verify application thread. We allow calls to this method from any thread.
    checkNotNull(listener);
    listeners.remove(listener);
    listenerArraySet.remove(listener);
  }

  public void setHandleWakeLock(boolean handleWakeLock) {
    setWakeMode(handleWakeLock ? C.WAKE_MODE_LOCAL : C.WAKE_MODE_NONE);
  }

  public void setWakeMode(@C.WakeMode int wakeMode) {
    verifyApplicationThread();
    switch (wakeMode) {
      case C.WAKE_MODE_NONE:
        wakeLockManager.setEnabled(false);
        wifiLockManager.setEnabled(false);
        break;
      case C.WAKE_MODE_LOCAL:
        wakeLockManager.setEnabled(true);
        wifiLockManager.setEnabled(false);
        break;
      case C.WAKE_MODE_NETWORK:
        wakeLockManager.setEnabled(true);
        wifiLockManager.setEnabled(true);
        break;
      default:
        break;
    }
  }

  public DeviceInfo getDeviceInfo() {
    verifyApplicationThread();
    return deviceInfo;
  }

  public int getDeviceVolume() {
    verifyApplicationThread();
    return streamVolumeManager.getVolume();
  }

  public boolean isDeviceMuted() {
    verifyApplicationThread();
    return streamVolumeManager.isMuted();
  }

  public void setDeviceVolume(int volume) {
    verifyApplicationThread();
    streamVolumeManager.setVolume(volume);
  }

  public void increaseDeviceVolume() {
    verifyApplicationThread();
    streamVolumeManager.increaseVolume();
  }

  public void decreaseDeviceVolume() {
    verifyApplicationThread();
    streamVolumeManager.decreaseVolume();
  }

  public void setDeviceMuted(boolean muted) {
    verifyApplicationThread();
    streamVolumeManager.setMuted(muted);
  }

  /* package */ void setThrowsWhenUsingWrongThread(boolean throwsWhenUsingWrongThread) {
    this.throwsWhenUsingWrongThread = throwsWhenUsingWrongThread;
  }

  private int getCurrentWindowIndexInternal() {
    if (playbackInfo.timeline.isEmpty()) {
      return maskingWindowIndex;
    } else {
      return playbackInfo.timeline.getPeriodByUid(playbackInfo.periodId.periodUid, period)
          .windowIndex;
    }
  }

  private long getCurrentPositionUsInternal(PlaybackInfo playbackInfo) {
    if (playbackInfo.timeline.isEmpty()) {
      return Util.msToUs(maskingWindowPositionMs);
    } else if (playbackInfo.periodId.isAd()) {
      return playbackInfo.positionUs;
    } else {
      return periodPositionUsToWindowPositionUs(
          playbackInfo.timeline, playbackInfo.periodId, playbackInfo.positionUs);
    }
  }

  private List<MediaSource> createMediaSources(List<MediaItem> mediaItems) {
    List<MediaSource> mediaSources = new ArrayList<>();
    for (int i = 0; i < mediaItems.size(); i++) {
      mediaSources.add(mediaSourceFactory.createMediaSource(mediaItems.get(i)));
    }
    return mediaSources;
  }

  private void handlePlaybackInfo(ExoPlayerImplInternal.PlaybackInfoUpdate playbackInfoUpdate) {
    pendingOperationAcks -= playbackInfoUpdate.operationAcks;
    if (playbackInfoUpdate.positionDiscontinuity) {
      pendingDiscontinuityReason = playbackInfoUpdate.discontinuityReason;
      pendingDiscontinuity = true;
    }
    if (playbackInfoUpdate.hasPlayWhenReadyChangeReason) {
      pendingPlayWhenReadyChangeReason = playbackInfoUpdate.playWhenReadyChangeReason;
    }
    if (pendingOperationAcks == 0) {
      Timeline newTimeline = playbackInfoUpdate.playbackInfo.timeline;
      if (!this.playbackInfo.timeline.isEmpty() && newTimeline.isEmpty()) {
        // Update the masking variables, which are used when the timeline becomes empty because a
        // ConcatenatingMediaSource has been cleared.
        maskingWindowIndex = C.INDEX_UNSET;
        maskingWindowPositionMs = 0;
        maskingPeriodIndex = 0;
      }
      if (!newTimeline.isEmpty()) {
        List<Timeline> timelines = ((PlaylistTimeline) newTimeline).getChildTimelines();
        checkState(timelines.size() == mediaSourceHolderSnapshots.size());
        for (int i = 0; i < timelines.size(); i++) {
          mediaSourceHolderSnapshots.get(i).timeline = timelines.get(i);
        }
      }
      boolean positionDiscontinuity = false;
      long discontinuityWindowStartPositionUs = C.TIME_UNSET;
      if (pendingDiscontinuity) {
        positionDiscontinuity =
            !playbackInfoUpdate.playbackInfo.periodId.equals(playbackInfo.periodId)
                || playbackInfoUpdate.playbackInfo.discontinuityStartPositionUs
                    != playbackInfo.positionUs;
        if (positionDiscontinuity) {
          discontinuityWindowStartPositionUs =
              newTimeline.isEmpty() || playbackInfoUpdate.playbackInfo.periodId.isAd()
                  ? playbackInfoUpdate.playbackInfo.discontinuityStartPositionUs
                  : periodPositionUsToWindowPositionUs(
                      newTimeline,
                      playbackInfoUpdate.playbackInfo.periodId,
                      playbackInfoUpdate.playbackInfo.discontinuityStartPositionUs);
        }
      }
      pendingDiscontinuity = false;
      updatePlaybackInfo(
          playbackInfoUpdate.playbackInfo,
          TIMELINE_CHANGE_REASON_SOURCE_UPDATE,
          pendingPlayWhenReadyChangeReason,
          /* seekProcessed= */ false,
          positionDiscontinuity,
          pendingDiscontinuityReason,
          discontinuityWindowStartPositionUs,
          /* ignored */ C.INDEX_UNSET);
    }
  }

  // Calling deprecated listeners.
  @SuppressWarnings("deprecation")
  private void updatePlaybackInfo(
      PlaybackInfo playbackInfo,
      @TimelineChangeReason int timelineChangeReason,
      @PlayWhenReadyChangeReason int playWhenReadyChangeReason,
      boolean seekProcessed,
      boolean positionDiscontinuity,
      @DiscontinuityReason int positionDiscontinuityReason,
      long discontinuityWindowStartPositionUs,
      int oldMaskingMediaItemIndex) {

    // Assign playback info immediately such that all getters return the right values, but keep
    // snapshot of previous and new state so that listener invocations are triggered correctly.
    PlaybackInfo previousPlaybackInfo = this.playbackInfo;
    PlaybackInfo newPlaybackInfo = playbackInfo;
    this.playbackInfo = playbackInfo;

    Pair<Boolean, Integer> mediaItemTransitionInfo =
        evaluateMediaItemTransitionReason(
            newPlaybackInfo,
            previousPlaybackInfo,
            positionDiscontinuity,
            positionDiscontinuityReason,
            !previousPlaybackInfo.timeline.equals(newPlaybackInfo.timeline));
    boolean mediaItemTransitioned = mediaItemTransitionInfo.first;
    int mediaItemTransitionReason = mediaItemTransitionInfo.second;
    MediaMetadata newMediaMetadata = mediaMetadata;
    @Nullable MediaItem mediaItem = null;
    if (mediaItemTransitioned) {
      if (!newPlaybackInfo.timeline.isEmpty()) {
        int windowIndex =
            newPlaybackInfo.timeline.getPeriodByUid(newPlaybackInfo.periodId.periodUid, period)
                .windowIndex;
        mediaItem = newPlaybackInfo.timeline.getWindow(windowIndex, window).mediaItem;
      }
      staticAndDynamicMediaMetadata = MediaMetadata.EMPTY;
    }
    if (mediaItemTransitioned
        || !previousPlaybackInfo.staticMetadata.equals(newPlaybackInfo.staticMetadata)) {
      staticAndDynamicMediaMetadata =
          staticAndDynamicMediaMetadata
              .buildUpon()
              .populateFromMetadata(newPlaybackInfo.staticMetadata)
              .build();
      newMediaMetadata = buildUpdatedMediaMetadata();
    }
    boolean metadataChanged = !newMediaMetadata.equals(mediaMetadata);
    mediaMetadata = newMediaMetadata;
    boolean playWhenReadyChanged =
        previousPlaybackInfo.playWhenReady != newPlaybackInfo.playWhenReady;
    boolean playbackStateChanged =
        previousPlaybackInfo.playbackState != newPlaybackInfo.playbackState;
    if (playbackStateChanged || playWhenReadyChanged) {
      updateWakeAndWifiLock();
    }
    boolean isLoadingChanged = previousPlaybackInfo.isLoading != newPlaybackInfo.isLoading;
    if (isLoadingChanged) {
      updatePriorityTaskManagerForIsLoadingChange(newPlaybackInfo.isLoading);
    }

    if (!previousPlaybackInfo.timeline.equals(newPlaybackInfo.timeline)) {
      listeners.queueEvent(
          Player.EVENT_TIMELINE_CHANGED,
          listener -> listener.onTimelineChanged(newPlaybackInfo.timeline, timelineChangeReason));
    }
    if (positionDiscontinuity) {
      PositionInfo previousPositionInfo =
          getPreviousPositionInfo(
              positionDiscontinuityReason, previousPlaybackInfo, oldMaskingMediaItemIndex);
      PositionInfo positionInfo = getPositionInfo(discontinuityWindowStartPositionUs);
      listeners.queueEvent(
          Player.EVENT_POSITION_DISCONTINUITY,
          listener -> {
            listener.onPositionDiscontinuity(positionDiscontinuityReason);
            listener.onPositionDiscontinuity(
                previousPositionInfo, positionInfo, positionDiscontinuityReason);
          });
    }
    if (mediaItemTransitioned) {
      @Nullable final MediaItem finalMediaItem = mediaItem;
      listeners.queueEvent(
          Player.EVENT_MEDIA_ITEM_TRANSITION,
          listener -> listener.onMediaItemTransition(finalMediaItem, mediaItemTransitionReason));
    }
    if (previousPlaybackInfo.playbackError != newPlaybackInfo.playbackError) {
      listeners.queueEvent(
          Player.EVENT_PLAYER_ERROR,
          listener -> listener.onPlayerErrorChanged(newPlaybackInfo.playbackError));
      if (newPlaybackInfo.playbackError != null) {
        listeners.queueEvent(
            Player.EVENT_PLAYER_ERROR,
            listener -> listener.onPlayerError(newPlaybackInfo.playbackError));
      }
    }
    if (previousPlaybackInfo.trackSelectorResult != newPlaybackInfo.trackSelectorResult) {
      trackSelector.onSelectionActivated(newPlaybackInfo.trackSelectorResult.info);
      TrackSelectionArray newSelection =
          new TrackSelectionArray(newPlaybackInfo.trackSelectorResult.selections);
      listeners.queueEvent(
          Player.EVENT_TRACKS_CHANGED,
          listener -> listener.onTracksChanged(newPlaybackInfo.trackGroups, newSelection));
      listeners.queueEvent(
          Player.EVENT_TRACKS_CHANGED,
          listener -> listener.onTracksInfoChanged(newPlaybackInfo.trackSelectorResult.tracksInfo));
    }
    if (metadataChanged) {
      final MediaMetadata finalMediaMetadata = mediaMetadata;
      listeners.queueEvent(
          EVENT_MEDIA_METADATA_CHANGED,
          listener -> listener.onMediaMetadataChanged(finalMediaMetadata));
    }
    if (isLoadingChanged) {
      listeners.queueEvent(
          Player.EVENT_IS_LOADING_CHANGED,
          listener -> {
            listener.onLoadingChanged(newPlaybackInfo.isLoading);
            listener.onIsLoadingChanged(newPlaybackInfo.isLoading);
          });
    }
    if (playbackStateChanged || playWhenReadyChanged) {
      listeners.queueEvent(
          /* eventFlag= */ C.INDEX_UNSET,
          listener ->
              listener.onPlayerStateChanged(
                  newPlaybackInfo.playWhenReady, newPlaybackInfo.playbackState));
    }
    if (playbackStateChanged) {
      listeners.queueEvent(
          Player.EVENT_PLAYBACK_STATE_CHANGED,
          listener -> listener.onPlaybackStateChanged(newPlaybackInfo.playbackState));
    }
    if (playWhenReadyChanged) {
      listeners.queueEvent(
          Player.EVENT_PLAY_WHEN_READY_CHANGED,
          listener ->
              listener.onPlayWhenReadyChanged(
                  newPlaybackInfo.playWhenReady, playWhenReadyChangeReason));
    }
    if (previousPlaybackInfo.playbackSuppressionReason
        != newPlaybackInfo.playbackSuppressionReason) {
      listeners.queueEvent(
          Player.EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED,
          listener ->
              listener.onPlaybackSuppressionReasonChanged(
                  newPlaybackInfo.playbackSuppressionReason));
    }
    if (isPlaying(previousPlaybackInfo) != isPlaying(newPlaybackInfo)) {
      listeners.queueEvent(
          Player.EVENT_IS_PLAYING_CHANGED,
          listener -> listener.onIsPlayingChanged(isPlaying(newPlaybackInfo)));
    }
    if (!previousPlaybackInfo.playbackParameters.equals(newPlaybackInfo.playbackParameters)) {
      listeners.queueEvent(
          Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
          listener -> listener.onPlaybackParametersChanged(newPlaybackInfo.playbackParameters));
    }
    if (seekProcessed) {
      listeners.queueEvent(/* eventFlag= */ C.INDEX_UNSET, Listener::onSeekProcessed);
    }
    updateAvailableCommands();
    listeners.flushEvents();

    if (previousPlaybackInfo.offloadSchedulingEnabled != newPlaybackInfo.offloadSchedulingEnabled) {
      for (AudioOffloadListener listener : audioOffloadListeners) {
        listener.onExperimentalOffloadSchedulingEnabledChanged(
            newPlaybackInfo.offloadSchedulingEnabled);
      }
    }
    if (previousPlaybackInfo.sleepingForOffload != newPlaybackInfo.sleepingForOffload) {
      for (AudioOffloadListener listener : audioOffloadListeners) {
        listener.onExperimentalSleepingForOffloadChanged(newPlaybackInfo.sleepingForOffload);
      }
    }
  }

  private PositionInfo getPreviousPositionInfo(
      @DiscontinuityReason int positionDiscontinuityReason,
      PlaybackInfo oldPlaybackInfo,
      int oldMaskingMediaItemIndex) {
    @Nullable Object oldWindowUid = null;
    @Nullable Object oldPeriodUid = null;
    int oldMediaItemIndex = oldMaskingMediaItemIndex;
    int oldPeriodIndex = C.INDEX_UNSET;
    @Nullable MediaItem oldMediaItem = null;
    Timeline.Period oldPeriod = new Timeline.Period();
    if (!oldPlaybackInfo.timeline.isEmpty()) {
      oldPeriodUid = oldPlaybackInfo.periodId.periodUid;
      oldPlaybackInfo.timeline.getPeriodByUid(oldPeriodUid, oldPeriod);
      oldMediaItemIndex = oldPeriod.windowIndex;
      oldPeriodIndex = oldPlaybackInfo.timeline.getIndexOfPeriod(oldPeriodUid);
      oldWindowUid = oldPlaybackInfo.timeline.getWindow(oldMediaItemIndex, window).uid;
      oldMediaItem = window.mediaItem;
    }
    long oldPositionUs;
    long oldContentPositionUs;
    if (positionDiscontinuityReason == DISCONTINUITY_REASON_AUTO_TRANSITION) {
      if (oldPlaybackInfo.periodId.isAd()) {
        // The old position is the end of the previous ad.
        oldPositionUs =
            oldPeriod.getAdDurationUs(
                oldPlaybackInfo.periodId.adGroupIndex, oldPlaybackInfo.periodId.adIndexInAdGroup);
        // The ad cue point is stored in the old requested content position.
        oldContentPositionUs = getRequestedContentPositionUs(oldPlaybackInfo);
      } else if (oldPlaybackInfo.periodId.nextAdGroupIndex != C.INDEX_UNSET) {
        // The old position is the end of a clipped content before an ad group. Use the exact ad
        // cue point as the transition position.
        oldPositionUs = getRequestedContentPositionUs(playbackInfo);
        oldContentPositionUs = oldPositionUs;
      } else {
        // The old position is the end of a Timeline period. Use the exact duration.
        oldPositionUs = oldPeriod.positionInWindowUs + oldPeriod.durationUs;
        oldContentPositionUs = oldPositionUs;
      }
    } else if (oldPlaybackInfo.periodId.isAd()) {
      oldPositionUs = oldPlaybackInfo.positionUs;
      oldContentPositionUs = getRequestedContentPositionUs(oldPlaybackInfo);
    } else {
      oldPositionUs = oldPeriod.positionInWindowUs + oldPlaybackInfo.positionUs;
      oldContentPositionUs = oldPositionUs;
    }
    return new PositionInfo(
        oldWindowUid,
        oldMediaItemIndex,
        oldMediaItem,
        oldPeriodUid,
        oldPeriodIndex,
        Util.usToMs(oldPositionUs),
        Util.usToMs(oldContentPositionUs),
        oldPlaybackInfo.periodId.adGroupIndex,
        oldPlaybackInfo.periodId.adIndexInAdGroup);
  }

  private PositionInfo getPositionInfo(long discontinuityWindowStartPositionUs) {
    @Nullable Object newWindowUid = null;
    @Nullable Object newPeriodUid = null;
    int newMediaItemIndex = getCurrentMediaItemIndex();
    int newPeriodIndex = C.INDEX_UNSET;
    @Nullable MediaItem newMediaItem = null;
    if (!playbackInfo.timeline.isEmpty()) {
      newPeriodUid = playbackInfo.periodId.periodUid;
      playbackInfo.timeline.getPeriodByUid(newPeriodUid, period);
      newPeriodIndex = playbackInfo.timeline.getIndexOfPeriod(newPeriodUid);
      newWindowUid = playbackInfo.timeline.getWindow(newMediaItemIndex, window).uid;
      newMediaItem = window.mediaItem;
    }
    long positionMs = Util.usToMs(discontinuityWindowStartPositionUs);
    return new PositionInfo(
        newWindowUid,
        newMediaItemIndex,
        newMediaItem,
        newPeriodUid,
        newPeriodIndex,
        positionMs,
        /* contentPositionMs= */ playbackInfo.periodId.isAd()
            ? Util.usToMs(getRequestedContentPositionUs(playbackInfo))
            : positionMs,
        playbackInfo.periodId.adGroupIndex,
        playbackInfo.periodId.adIndexInAdGroup);
  }

  private static long getRequestedContentPositionUs(PlaybackInfo playbackInfo) {
    Timeline.Window window = new Timeline.Window();
    Timeline.Period period = new Timeline.Period();
    playbackInfo.timeline.getPeriodByUid(playbackInfo.periodId.periodUid, period);
    return playbackInfo.requestedContentPositionUs == C.TIME_UNSET
        ? playbackInfo.timeline.getWindow(period.windowIndex, window).getDefaultPositionUs()
        : period.getPositionInWindowUs() + playbackInfo.requestedContentPositionUs;
  }

  private Pair<Boolean, Integer> evaluateMediaItemTransitionReason(
      PlaybackInfo playbackInfo,
      PlaybackInfo oldPlaybackInfo,
      boolean positionDiscontinuity,
      @DiscontinuityReason int positionDiscontinuityReason,
      boolean timelineChanged) {

    Timeline oldTimeline = oldPlaybackInfo.timeline;
    Timeline newTimeline = playbackInfo.timeline;
    if (newTimeline.isEmpty() && oldTimeline.isEmpty()) {
      return new Pair<>(/* isTransitioning */ false, /* mediaItemTransitionReason */ C.INDEX_UNSET);
    } else if (newTimeline.isEmpty() != oldTimeline.isEmpty()) {
      return new Pair<>(/* isTransitioning */ true, MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED);
    }

    int oldWindowIndex =
        oldTimeline.getPeriodByUid(oldPlaybackInfo.periodId.periodUid, period).windowIndex;
    Object oldWindowUid = oldTimeline.getWindow(oldWindowIndex, window).uid;
    int newWindowIndex =
        newTimeline.getPeriodByUid(playbackInfo.periodId.periodUid, period).windowIndex;
    Object newWindowUid = newTimeline.getWindow(newWindowIndex, window).uid;
    if (!oldWindowUid.equals(newWindowUid)) {
      @Player.MediaItemTransitionReason int transitionReason;
      if (positionDiscontinuity
          && positionDiscontinuityReason == DISCONTINUITY_REASON_AUTO_TRANSITION) {
        transitionReason = MEDIA_ITEM_TRANSITION_REASON_AUTO;
      } else if (positionDiscontinuity
          && positionDiscontinuityReason == DISCONTINUITY_REASON_SEEK) {
        transitionReason = MEDIA_ITEM_TRANSITION_REASON_SEEK;
      } else if (timelineChanged) {
        transitionReason = MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED;
      } else {
        // A change in window uid must be justified by one of the reasons above.
        throw new IllegalStateException();
      }
      return new Pair<>(/* isTransitioning */ true, transitionReason);
    } else if (positionDiscontinuity
        && positionDiscontinuityReason == DISCONTINUITY_REASON_AUTO_TRANSITION
        && oldPlaybackInfo.periodId.windowSequenceNumber
            < playbackInfo.periodId.windowSequenceNumber) {
      return new Pair<>(/* isTransitioning */ true, MEDIA_ITEM_TRANSITION_REASON_REPEAT);
    }
    return new Pair<>(/* isTransitioning */ false, /* mediaItemTransitionReason */ C.INDEX_UNSET);
  }

  private void updateAvailableCommands() {
    Commands previousAvailableCommands = availableCommands;
    availableCommands = Util.getAvailableCommands(wrappingPlayer, permanentAvailableCommands);
    if (!availableCommands.equals(previousAvailableCommands)) {
      listeners.queueEvent(
          Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
          listener -> listener.onAvailableCommandsChanged(availableCommands));
    }
  }

  private void setMediaSourcesInternal(
      List<MediaSource> mediaSources,
      int startWindowIndex,
      long startPositionMs,
      boolean resetToDefaultPosition) {
    int currentWindowIndex = getCurrentWindowIndexInternal();
    long currentPositionMs = getCurrentPosition();
    pendingOperationAcks++;
    if (!mediaSourceHolderSnapshots.isEmpty()) {
      removeMediaSourceHolders(
          /* fromIndex= */ 0, /* toIndexExclusive= */ mediaSourceHolderSnapshots.size());
    }
    List<MediaSourceList.MediaSourceHolder> holders =
        addMediaSourceHolders(/* index= */ 0, mediaSources);
    Timeline timeline = createMaskingTimeline();
    if (!timeline.isEmpty() && startWindowIndex >= timeline.getWindowCount()) {
      throw new IllegalSeekPositionException(timeline, startWindowIndex, startPositionMs);
    }
    // Evaluate the actual start position.
    if (resetToDefaultPosition) {
      startWindowIndex = timeline.getFirstWindowIndex(shuffleModeEnabled);
      startPositionMs = C.TIME_UNSET;
    } else if (startWindowIndex == C.INDEX_UNSET) {
      startWindowIndex = currentWindowIndex;
      startPositionMs = currentPositionMs;
    }
    PlaybackInfo newPlaybackInfo =
        maskTimelineAndPosition(
            playbackInfo,
            timeline,
            maskWindowPositionMsOrGetPeriodPositionUs(timeline, startWindowIndex, startPositionMs));
    // Mask the playback state.
    int maskingPlaybackState = newPlaybackInfo.playbackState;
    if (startWindowIndex != C.INDEX_UNSET && newPlaybackInfo.playbackState != STATE_IDLE) {
      // Position reset to startWindowIndex (results in pending initial seek).
      if (timeline.isEmpty() || startWindowIndex >= timeline.getWindowCount()) {
        // Setting an empty timeline or invalid seek transitions to ended.
        maskingPlaybackState = STATE_ENDED;
      } else {
        maskingPlaybackState = STATE_BUFFERING;
      }
    }
    newPlaybackInfo = newPlaybackInfo.copyWithPlaybackState(maskingPlaybackState);
    internalPlayer.setMediaSources(
        holders, startWindowIndex, Util.msToUs(startPositionMs), shuffleOrder);
    boolean positionDiscontinuity =
        !playbackInfo.periodId.periodUid.equals(newPlaybackInfo.periodId.periodUid)
            && !playbackInfo.timeline.isEmpty();
    updatePlaybackInfo(
        newPlaybackInfo,
        /* timelineChangeReason= */ TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
        /* ignored */ PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
        /* seekProcessed= */ false,
        /* positionDiscontinuity= */ positionDiscontinuity,
        DISCONTINUITY_REASON_REMOVE,
        /* discontinuityWindowStartPositionUs= */ getCurrentPositionUsInternal(newPlaybackInfo),
        /* ignored */ C.INDEX_UNSET);
  }

  private List<MediaSourceList.MediaSourceHolder> addMediaSourceHolders(
      int index, List<MediaSource> mediaSources) {
    List<MediaSourceList.MediaSourceHolder> holders = new ArrayList<>();
    for (int i = 0; i < mediaSources.size(); i++) {
      MediaSourceList.MediaSourceHolder holder =
          new MediaSourceList.MediaSourceHolder(mediaSources.get(i), useLazyPreparation);
      holders.add(holder);
      mediaSourceHolderSnapshots.add(
          i + index, new MediaSourceHolderSnapshot(holder.uid, holder.mediaSource.getTimeline()));
    }
    shuffleOrder =
        shuffleOrder.cloneAndInsert(
            /* insertionIndex= */ index, /* insertionCount= */ holders.size());
    return holders;
  }

  private PlaybackInfo removeMediaItemsInternal(int fromIndex, int toIndex) {
    Assertions.checkArgument(
        fromIndex >= 0 && toIndex >= fromIndex && toIndex <= mediaSourceHolderSnapshots.size());
    int currentIndex = getCurrentMediaItemIndex();
    Timeline oldTimeline = getCurrentTimeline();
    int currentMediaSourceCount = mediaSourceHolderSnapshots.size();
    pendingOperationAcks++;
    removeMediaSourceHolders(fromIndex, /* toIndexExclusive= */ toIndex);
    Timeline newTimeline = createMaskingTimeline();
    PlaybackInfo newPlaybackInfo =
        maskTimelineAndPosition(
            playbackInfo,
            newTimeline,
            getPeriodPositionUsAfterTimelineChanged(oldTimeline, newTimeline));
    // Player transitions to STATE_ENDED if the current index is part of the removed tail.
    final boolean transitionsToEnded =
        newPlaybackInfo.playbackState != STATE_IDLE
            && newPlaybackInfo.playbackState != STATE_ENDED
            && fromIndex < toIndex
            && toIndex == currentMediaSourceCount
            && currentIndex >= newPlaybackInfo.timeline.getWindowCount();
    if (transitionsToEnded) {
      newPlaybackInfo = newPlaybackInfo.copyWithPlaybackState(STATE_ENDED);
    }
    internalPlayer.removeMediaSources(fromIndex, toIndex, shuffleOrder);
    return newPlaybackInfo;
  }

  private void removeMediaSourceHolders(int fromIndex, int toIndexExclusive) {
    for (int i = toIndexExclusive - 1; i >= fromIndex; i--) {
      mediaSourceHolderSnapshots.remove(i);
    }
    shuffleOrder = shuffleOrder.cloneAndRemove(fromIndex, toIndexExclusive);
  }

  private Timeline createMaskingTimeline() {
    return new PlaylistTimeline(mediaSourceHolderSnapshots, shuffleOrder);
  }

  private PlaybackInfo maskTimelineAndPosition(
      PlaybackInfo playbackInfo, Timeline timeline, @Nullable Pair<Object, Long> periodPositionUs) {
    Assertions.checkArgument(timeline.isEmpty() || periodPositionUs != null);
    Timeline oldTimeline = playbackInfo.timeline;
    // Mask the timeline.
    playbackInfo = playbackInfo.copyWithTimeline(timeline);

    if (timeline.isEmpty()) {
      // Reset periodId and loadingPeriodId.
      MediaPeriodId dummyMediaPeriodId = PlaybackInfo.getDummyPeriodForEmptyTimeline();
      long positionUs = Util.msToUs(maskingWindowPositionMs);
      playbackInfo =
          playbackInfo.copyWithNewPosition(
              dummyMediaPeriodId,
              positionUs,
              /* requestedContentPositionUs= */ positionUs,
              /* discontinuityStartPositionUs= */ positionUs,
              /* totalBufferedDurationUs= */ 0,
              TrackGroupArray.EMPTY,
              emptyTrackSelectorResult,
              /* staticMetadata= */ ImmutableList.of());
      playbackInfo = playbackInfo.copyWithLoadingMediaPeriodId(dummyMediaPeriodId);
      playbackInfo.bufferedPositionUs = playbackInfo.positionUs;
      return playbackInfo;
    }

    Object oldPeriodUid = playbackInfo.periodId.periodUid;
    boolean playingPeriodChanged = !oldPeriodUid.equals(castNonNull(periodPositionUs).first);
    MediaPeriodId newPeriodId =
        playingPeriodChanged ? new MediaPeriodId(periodPositionUs.first) : playbackInfo.periodId;
    long newContentPositionUs = periodPositionUs.second;
    long oldContentPositionUs = Util.msToUs(getContentPosition());
    if (!oldTimeline.isEmpty()) {
      oldContentPositionUs -=
          oldTimeline.getPeriodByUid(oldPeriodUid, period).getPositionInWindowUs();
    }

    if (playingPeriodChanged || newContentPositionUs < oldContentPositionUs) {
      checkState(!newPeriodId.isAd());
      // The playing period changes or a backwards seek within the playing period occurs.
      playbackInfo =
          playbackInfo.copyWithNewPosition(
              newPeriodId,
              /* positionUs= */ newContentPositionUs,
              /* requestedContentPositionUs= */ newContentPositionUs,
              /* discontinuityStartPositionUs= */ newContentPositionUs,
              /* totalBufferedDurationUs= */ 0,
              playingPeriodChanged ? TrackGroupArray.EMPTY : playbackInfo.trackGroups,
              playingPeriodChanged ? emptyTrackSelectorResult : playbackInfo.trackSelectorResult,
              playingPeriodChanged ? ImmutableList.of() : playbackInfo.staticMetadata);
      playbackInfo = playbackInfo.copyWithLoadingMediaPeriodId(newPeriodId);
      playbackInfo.bufferedPositionUs = newContentPositionUs;
    } else if (newContentPositionUs == oldContentPositionUs) {
      // Period position remains unchanged.
      int loadingPeriodIndex =
          timeline.getIndexOfPeriod(playbackInfo.loadingMediaPeriodId.periodUid);
      if (loadingPeriodIndex == C.INDEX_UNSET
          || timeline.getPeriod(loadingPeriodIndex, period).windowIndex
              != timeline.getPeriodByUid(newPeriodId.periodUid, period).windowIndex) {
        // Discard periods after the playing period, if the loading period is discarded or the
        // playing and loading period are not in the same window.
        timeline.getPeriodByUid(newPeriodId.periodUid, period);
        long maskedBufferedPositionUs =
            newPeriodId.isAd()
                ? period.getAdDurationUs(newPeriodId.adGroupIndex, newPeriodId.adIndexInAdGroup)
                : period.durationUs;
        playbackInfo =
            playbackInfo.copyWithNewPosition(
                newPeriodId,
                /* positionUs= */ playbackInfo.positionUs,
                /* requestedContentPositionUs= */ playbackInfo.positionUs,
                playbackInfo.discontinuityStartPositionUs,
                /* totalBufferedDurationUs= */ maskedBufferedPositionUs - playbackInfo.positionUs,
                playbackInfo.trackGroups,
                playbackInfo.trackSelectorResult,
                playbackInfo.staticMetadata);
        playbackInfo = playbackInfo.copyWithLoadingMediaPeriodId(newPeriodId);
        playbackInfo.bufferedPositionUs = maskedBufferedPositionUs;
      }
    } else {
      checkState(!newPeriodId.isAd());
      // A forward seek within the playing period (timeline did not change).
      long maskedTotalBufferedDurationUs =
          max(
              0,
              playbackInfo.totalBufferedDurationUs - (newContentPositionUs - oldContentPositionUs));
      long maskedBufferedPositionUs = playbackInfo.bufferedPositionUs;
      if (playbackInfo.loadingMediaPeriodId.equals(playbackInfo.periodId)) {
        maskedBufferedPositionUs = newContentPositionUs + maskedTotalBufferedDurationUs;
      }
      playbackInfo =
          playbackInfo.copyWithNewPosition(
              newPeriodId,
              /* positionUs= */ newContentPositionUs,
              /* requestedContentPositionUs= */ newContentPositionUs,
              /* discontinuityStartPositionUs= */ newContentPositionUs,
              maskedTotalBufferedDurationUs,
              playbackInfo.trackGroups,
              playbackInfo.trackSelectorResult,
              playbackInfo.staticMetadata);
      playbackInfo.bufferedPositionUs = maskedBufferedPositionUs;
    }
    return playbackInfo;
  }

  @Nullable
  private Pair<Object, Long> getPeriodPositionUsAfterTimelineChanged(
      Timeline oldTimeline, Timeline newTimeline) {
    long currentPositionMs = getContentPosition();
    if (oldTimeline.isEmpty() || newTimeline.isEmpty()) {
      boolean isCleared = !oldTimeline.isEmpty() && newTimeline.isEmpty();
      return maskWindowPositionMsOrGetPeriodPositionUs(
          newTimeline,
          isCleared ? C.INDEX_UNSET : getCurrentWindowIndexInternal(),
          isCleared ? C.TIME_UNSET : currentPositionMs);
    }
    int currentMediaItemIndex = getCurrentMediaItemIndex();
    @Nullable
    Pair<Object, Long> oldPeriodPositionUs =
        oldTimeline.getPeriodPositionUs(
            window, period, currentMediaItemIndex, Util.msToUs(currentPositionMs));
    Object periodUid = castNonNull(oldPeriodPositionUs).first;
    if (newTimeline.getIndexOfPeriod(periodUid) != C.INDEX_UNSET) {
      // The old period position is still available in the new timeline.
      return oldPeriodPositionUs;
    }
    // Period uid not found in new timeline. Try to get subsequent period.
    @Nullable
    Object nextPeriodUid =
        ExoPlayerImplInternal.resolveSubsequentPeriod(
            window, period, repeatMode, shuffleModeEnabled, periodUid, oldTimeline, newTimeline);
    if (nextPeriodUid != null) {
      // Reset position to the default position of the window of the subsequent period.
      newTimeline.getPeriodByUid(nextPeriodUid, period);
      return maskWindowPositionMsOrGetPeriodPositionUs(
          newTimeline,
          period.windowIndex,
          newTimeline.getWindow(period.windowIndex, window).getDefaultPositionMs());
    } else {
      // No subsequent period found and the new timeline is not empty. Use the default position.
      return maskWindowPositionMsOrGetPeriodPositionUs(
          newTimeline, /* windowIndex= */ C.INDEX_UNSET, /* windowPositionMs= */ C.TIME_UNSET);
    }
  }

  @Nullable
  private Pair<Object, Long> maskWindowPositionMsOrGetPeriodPositionUs(
      Timeline timeline, int windowIndex, long windowPositionMs) {
    if (timeline.isEmpty()) {
      // If empty we store the initial seek in the masking variables.
      maskingWindowIndex = windowIndex;
      maskingWindowPositionMs = windowPositionMs == C.TIME_UNSET ? 0 : windowPositionMs;
      maskingPeriodIndex = 0;
      return null;
    }
    if (windowIndex == C.INDEX_UNSET || windowIndex >= timeline.getWindowCount()) {
      // Use default position of timeline if window index still unset or if a previous initial seek
      // now turns out to be invalid.
      windowIndex = timeline.getFirstWindowIndex(shuffleModeEnabled);
      windowPositionMs = timeline.getWindow(windowIndex, window).getDefaultPositionMs();
    }
    return timeline.getPeriodPositionUs(window, period, windowIndex, Util.msToUs(windowPositionMs));
  }

  private long periodPositionUsToWindowPositionUs(
      Timeline timeline, MediaPeriodId periodId, long positionUs) {
    timeline.getPeriodByUid(periodId.periodUid, period);
    positionUs += period.getPositionInWindowUs();
    return positionUs;
  }

  private PlayerMessage createMessageInternal(Target target) {
    int currentWindowIndex = getCurrentWindowIndexInternal();
    return new PlayerMessage(
        internalPlayer,
        target,
        playbackInfo.timeline,
        currentWindowIndex == C.INDEX_UNSET ? 0 : currentWindowIndex,
        clock,
        internalPlayer.getPlaybackLooper());
  }

  /**
   * Builds a {@link MediaMetadata} from the main sources.
   *
   * <p>{@link MediaItem} {@link MediaMetadata} is prioritized, with any gaps/missing fields
   * populated by metadata from static ({@link TrackGroup} {@link Format}) and dynamic ({@link
   * MetadataOutput#onMetadata(Metadata)}) sources.
   */
  private MediaMetadata buildUpdatedMediaMetadata() {
    Timeline timeline = getCurrentTimeline();
    if (timeline.isEmpty()) {
      return staticAndDynamicMediaMetadata;
    }
    MediaItem mediaItem = timeline.getWindow(getCurrentMediaItemIndex(), window).mediaItem;
    // MediaItem metadata is prioritized over metadata within the media.
    return staticAndDynamicMediaMetadata.buildUpon().populate(mediaItem.mediaMetadata).build();
  }

  private void removeSurfaceCallbacks() {
    if (sphericalGLSurfaceView != null) {
      createMessageInternal(frameMetadataListener)
          .setType(FrameMetadataListener.MSG_SET_SPHERICAL_SURFACE_VIEW)
          .setPayload(null)
          .send();
      sphericalGLSurfaceView.removeVideoSurfaceListener(componentListener);
      sphericalGLSurfaceView = null;
    }
    if (textureView != null) {
      if (textureView.getSurfaceTextureListener() != componentListener) {
        Log.w(TAG, "SurfaceTextureListener already unset or replaced.");
      } else {
        textureView.setSurfaceTextureListener(null);
      }
      textureView = null;
    }
    if (surfaceHolder != null) {
      surfaceHolder.removeCallback(componentListener);
      surfaceHolder = null;
    }
  }

  private void setSurfaceTextureInternal(SurfaceTexture surfaceTexture) {
    Surface surface = new Surface(surfaceTexture);
    setVideoOutputInternal(surface);
    ownedSurface = surface;
  }

  private void setVideoOutputInternal(@Nullable Object videoOutput) {
    // Note: We don't turn this method into a no-op if the output is being replaced with itself so
    // as to ensure onRenderedFirstFrame callbacks are still called in this case.
    List<PlayerMessage> messages = new ArrayList<>();
    for (Renderer renderer : renderers) {
      if (renderer.getTrackType() == TRACK_TYPE_VIDEO) {
        messages.add(
            createMessageInternal(renderer)
                .setType(MSG_SET_VIDEO_OUTPUT)
                .setPayload(videoOutput)
                .send());
      }
    }
    boolean messageDeliveryTimedOut = false;
    if (this.videoOutput != null && this.videoOutput != videoOutput) {
      // We're replacing an output. Block to ensure that this output will not be accessed by the
      // renderers after this method returns.
      try {
        for (PlayerMessage message : messages) {
          message.blockUntilDelivered(detachSurfaceTimeoutMs);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (TimeoutException e) {
        messageDeliveryTimedOut = true;
      }
      if (this.videoOutput == ownedSurface) {
        // We're replacing a surface that we are responsible for releasing.
        ownedSurface.release();
        ownedSurface = null;
      }
    }
    this.videoOutput = videoOutput;
    if (messageDeliveryTimedOut) {
      stop(
          /* reset= */ false,
          ExoPlaybackException.createForUnexpected(
              new ExoTimeoutException(ExoTimeoutException.TIMEOUT_OPERATION_DETACH_SURFACE),
              PlaybackException.ERROR_CODE_TIMEOUT));
    }
  }

  /**
   * Sets the holder of the surface that will be displayed to the user, but which should
   * <em>not</em> be the output for video renderers. This case occurs when video frames need to be
   * rendered to an intermediate surface (which is not the one held by the provided holder).
   *
   * @param nonVideoOutputSurfaceHolder The holder of the surface that will eventually be displayed
   *     to the user.
   */
  private void setNonVideoOutputSurfaceHolderInternal(SurfaceHolder nonVideoOutputSurfaceHolder) {
    // Although we won't use the view's surface directly as the video output, still use the holder
    // to query the surface size, to be informed in changes to the size via componentListener, and
    // for equality checking in clearVideoSurfaceHolder.
    surfaceHolderSurfaceIsVideoOutput = false;
    surfaceHolder = nonVideoOutputSurfaceHolder;
    surfaceHolder.addCallback(componentListener);
    Surface surface = surfaceHolder.getSurface();
    if (surface != null && surface.isValid()) {
      Rect surfaceSize = surfaceHolder.getSurfaceFrame();
      maybeNotifySurfaceSizeChanged(surfaceSize.width(), surfaceSize.height());
    } else {
      maybeNotifySurfaceSizeChanged(/* width= */ 0, /* height= */ 0);
    }
  }

  private void maybeNotifySurfaceSizeChanged(int width, int height) {
    if (width != surfaceWidth || height != surfaceHeight) {
      surfaceWidth = width;
      surfaceHeight = height;
      // TODO(internal b/187152483): Events should be dispatched via ListenerSet
      for (Listener listener : listenerArraySet) {
        listener.onSurfaceSizeChanged(width, height);
      }
    }
  }

  private void sendVolumeToRenderers() {
    float scaledVolume = volume * audioFocusManager.getVolumeMultiplier();
    sendRendererMessage(TRACK_TYPE_AUDIO, MSG_SET_VOLUME, scaledVolume);
  }

  private void notifySkipSilenceEnabledChanged() {
    // TODO(internal b/187152483): Events should be dispatched via ListenerSet
    for (Listener listener : listenerArraySet) {
      listener.onSkipSilenceEnabledChanged(skipSilenceEnabled);
    }
  }

  private void updatePlayWhenReady(
      boolean playWhenReady,
      @AudioFocusManager.PlayerCommand int playerCommand,
      @Player.PlayWhenReadyChangeReason int playWhenReadyChangeReason) {
    playWhenReady = playWhenReady && playerCommand != AudioFocusManager.PLAYER_COMMAND_DO_NOT_PLAY;
    @PlaybackSuppressionReason
    int playbackSuppressionReason =
        playWhenReady && playerCommand != AudioFocusManager.PLAYER_COMMAND_PLAY_WHEN_READY
            ? Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
            : Player.PLAYBACK_SUPPRESSION_REASON_NONE;
    if (playbackInfo.playWhenReady == playWhenReady
        && playbackInfo.playbackSuppressionReason == playbackSuppressionReason) {
      return;
    }
    pendingOperationAcks++;
    PlaybackInfo playbackInfo =
        this.playbackInfo.copyWithPlayWhenReady(playWhenReady, playbackSuppressionReason);
    internalPlayer.setPlayWhenReady(playWhenReady, playbackSuppressionReason);
    updatePlaybackInfo(
        playbackInfo,
        /* ignored */ TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
        playWhenReadyChangeReason,
        /* seekProcessed= */ false,
        /* positionDiscontinuity= */ false,
        /* ignored */ DISCONTINUITY_REASON_INTERNAL,
        /* ignored */ C.TIME_UNSET,
        /* ignored */ C.INDEX_UNSET);
  }

  private void updateWakeAndWifiLock() {
    @State int playbackState = getPlaybackState();
    switch (playbackState) {
      case Player.STATE_READY:
      case Player.STATE_BUFFERING:
        boolean isSleeping = experimentalIsSleepingForOffload();
        wakeLockManager.setStayAwake(getPlayWhenReady() && !isSleeping);
        // The wifi lock is not released while sleeping to avoid interrupting downloads.
        wifiLockManager.setStayAwake(getPlayWhenReady());
        break;
      case Player.STATE_ENDED:
      case Player.STATE_IDLE:
        wakeLockManager.setStayAwake(false);
        wifiLockManager.setStayAwake(false);
        break;
      default:
        throw new IllegalStateException();
    }
  }

  private void verifyApplicationThread() {
    // The constructor may be executed on a background thread. Wait with accessing the player from
    // the app thread until the constructor finished executing.
    constructorFinished.blockUninterruptible();
    if (Thread.currentThread() != getApplicationLooper().getThread()) {
      String message =
          Util.formatInvariant(
              "Player is accessed on the wrong thread.\n"
                  + "Current thread: '%s'\n"
                  + "Expected thread: '%s'\n"
                  + "See https://exoplayer.dev/issues/player-accessed-on-wrong-thread",
              Thread.currentThread().getName(), getApplicationLooper().getThread().getName());
      if (throwsWhenUsingWrongThread) {
        throw new IllegalStateException(message);
      }
      Log.w(TAG, message, hasNotifiedFullWrongThreadWarning ? null : new IllegalStateException());
      hasNotifiedFullWrongThreadWarning = true;
    }
  }

  private void sendRendererMessage(
      @C.TrackType int trackType, int messageType, @Nullable Object payload) {
    for (Renderer renderer : renderers) {
      if (renderer.getTrackType() == trackType) {
        createMessageInternal(renderer).setType(messageType).setPayload(payload).send();
      }
    }
  }

  /**
   * Initializes {@link #keepSessionIdAudioTrack} to keep an audio session ID alive. If the audio
   * session ID is {@link C#AUDIO_SESSION_ID_UNSET} then a new audio session ID is generated.
   *
   * <p>Use of this method is only required on API level 21 and earlier.
   *
   * @param audioSessionId The audio session ID, or {@link C#AUDIO_SESSION_ID_UNSET} to generate a
   *     new one.
   * @return The audio session ID.
   */
  private int initializeKeepSessionIdAudioTrack(int audioSessionId) {
    if (keepSessionIdAudioTrack != null
        && keepSessionIdAudioTrack.getAudioSessionId() != audioSessionId) {
      keepSessionIdAudioTrack.release();
      keepSessionIdAudioTrack = null;
    }
    if (keepSessionIdAudioTrack == null) {
      int sampleRate = 4000; // Minimum sample rate supported by the platform.
      int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
      @C.PcmEncoding int encoding = C.ENCODING_PCM_16BIT;
      int bufferSize = 2; // Use a two byte buffer, as it is not actually used for playback.
      keepSessionIdAudioTrack =
          new AudioTrack(
              C.STREAM_TYPE_DEFAULT,
              sampleRate,
              channelConfig,
              encoding,
              bufferSize,
              AudioTrack.MODE_STATIC,
              audioSessionId);
    }
    return keepSessionIdAudioTrack.getAudioSessionId();
  }

  private void updatePriorityTaskManagerForIsLoadingChange(boolean isLoading) {
    if (priorityTaskManager != null) {
      if (isLoading && !isPriorityTaskManagerRegistered) {
        priorityTaskManager.add(C.PRIORITY_PLAYBACK);
        isPriorityTaskManagerRegistered = true;
      } else if (!isLoading && isPriorityTaskManagerRegistered) {
        priorityTaskManager.remove(C.PRIORITY_PLAYBACK);
        isPriorityTaskManagerRegistered = false;
      }
    }
  }

  private static DeviceInfo createDeviceInfo(StreamVolumeManager streamVolumeManager) {
    return new DeviceInfo(
        DeviceInfo.PLAYBACK_TYPE_LOCAL,
        streamVolumeManager.getMinVolume(),
        streamVolumeManager.getMaxVolume());
  }

  private static int getPlayWhenReadyChangeReason(boolean playWhenReady, int playerCommand) {
    return playWhenReady && playerCommand != AudioFocusManager.PLAYER_COMMAND_PLAY_WHEN_READY
        ? PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS
        : PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST;
  }

  private static boolean isPlaying(PlaybackInfo playbackInfo) {
    return playbackInfo.playbackState == Player.STATE_READY
        && playbackInfo.playWhenReady
        && playbackInfo.playbackSuppressionReason == PLAYBACK_SUPPRESSION_REASON_NONE;
  }

  private static final class MediaSourceHolderSnapshot implements MediaSourceInfoHolder {

    private final Object uid;

    private Timeline timeline;

    public MediaSourceHolderSnapshot(Object uid, Timeline timeline) {
      this.uid = uid;
      this.timeline = timeline;
    }

    @Override
    public Object getUid() {
      return uid;
    }

    @Override
    public Timeline getTimeline() {
      return timeline;
    }
  }

  private final class ComponentListener
      implements VideoRendererEventListener,
          AudioRendererEventListener,
          TextOutput,
          MetadataOutput,
          SurfaceHolder.Callback,
          TextureView.SurfaceTextureListener,
          SphericalGLSurfaceView.VideoSurfaceListener,
          AudioFocusManager.PlayerControl,
          AudioBecomingNoisyManager.EventListener,
          StreamVolumeManager.Listener,
          AudioOffloadListener {

    // VideoRendererEventListener implementation

    @Override
    public void onVideoEnabled(DecoderCounters counters) {
      videoDecoderCounters = counters;
      analyticsCollector.onVideoEnabled(counters);
    }

    @Override
    public void onVideoDecoderInitialized(
        String decoderName, long initializedTimestampMs, long initializationDurationMs) {
      analyticsCollector.onVideoDecoderInitialized(
          decoderName, initializedTimestampMs, initializationDurationMs);
    }

    @Override
    public void onVideoInputFormatChanged(
        Format format, @Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
      videoFormat = format;
      analyticsCollector.onVideoInputFormatChanged(format, decoderReuseEvaluation);
    }

    @Override
    public void onDroppedFrames(int count, long elapsed) {
      analyticsCollector.onDroppedFrames(count, elapsed);
    }

    @Override
    public void onVideoSizeChanged(VideoSize videoSize) {
      ExoPlayerImpl.this.videoSize = videoSize;
      // TODO(internal b/187152483): Events should be dispatched via ListenerSet
      for (Listener listener : listenerArraySet) {
        listener.onVideoSizeChanged(videoSize);
      }
    }

    @Override
    public void onRenderedFirstFrame(Object output, long renderTimeMs) {
      analyticsCollector.onRenderedFirstFrame(output, renderTimeMs);
      if (videoOutput == output) {
        // TODO(internal b/187152483): Events should be dispatched via ListenerSet
        for (Listener listener : listenerArraySet) {
          listener.onRenderedFirstFrame();
        }
      }
    }

    @Override
    public void onVideoDecoderReleased(String decoderName) {
      analyticsCollector.onVideoDecoderReleased(decoderName);
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {
      analyticsCollector.onVideoDisabled(counters);
      videoFormat = null;
      videoDecoderCounters = null;
    }

    @Override
    public void onVideoFrameProcessingOffset(long totalProcessingOffsetUs, int frameCount) {
      analyticsCollector.onVideoFrameProcessingOffset(totalProcessingOffsetUs, frameCount);
    }

    @Override
    public void onVideoCodecError(Exception videoCodecError) {
      analyticsCollector.onVideoCodecError(videoCodecError);
    }

    // AudioRendererEventListener implementation

    @Override
    public void onAudioEnabled(DecoderCounters counters) {
      audioDecoderCounters = counters;
      analyticsCollector.onAudioEnabled(counters);
    }

    @Override
    public void onAudioDecoderInitialized(
        String decoderName, long initializedTimestampMs, long initializationDurationMs) {
      analyticsCollector.onAudioDecoderInitialized(
          decoderName, initializedTimestampMs, initializationDurationMs);
    }

    @Override
    public void onAudioInputFormatChanged(
        Format format, @Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
      audioFormat = format;
      analyticsCollector.onAudioInputFormatChanged(format, decoderReuseEvaluation);
    }

    @Override
    public void onAudioPositionAdvancing(long playoutStartSystemTimeMs) {
      analyticsCollector.onAudioPositionAdvancing(playoutStartSystemTimeMs);
    }

    @Override
    public void onAudioUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
      analyticsCollector.onAudioUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
    }

    @Override
    public void onAudioDecoderReleased(String decoderName) {
      analyticsCollector.onAudioDecoderReleased(decoderName);
    }

    @Override
    public void onAudioDisabled(DecoderCounters counters) {
      analyticsCollector.onAudioDisabled(counters);
      audioFormat = null;
      audioDecoderCounters = null;
    }

    @Override
    public void onSkipSilenceEnabledChanged(boolean skipSilenceEnabled) {
      if (ExoPlayerImpl.this.skipSilenceEnabled == skipSilenceEnabled) {
        return;
      }
      ExoPlayerImpl.this.skipSilenceEnabled = skipSilenceEnabled;
      notifySkipSilenceEnabledChanged();
    }

    @Override
    public void onAudioSinkError(Exception audioSinkError) {
      analyticsCollector.onAudioSinkError(audioSinkError);
    }

    @Override
    public void onAudioCodecError(Exception audioCodecError) {
      analyticsCollector.onAudioCodecError(audioCodecError);
    }

    // TextOutput implementation

    @Override
    public void onCues(List<Cue> cues) {
      currentCues = cues;
      // TODO(internal b/187152483): Events should be dispatched via ListenerSet
      for (Listener listeners : listenerArraySet) {
        listeners.onCues(cues);
      }
    }

    // MetadataOutput implementation

    @Override
    public void onMetadata(Metadata metadata) {
      staticAndDynamicMediaMetadata =
          staticAndDynamicMediaMetadata.buildUpon().populateFromMetadata(metadata).build();
      MediaMetadata newMediaMetadata = buildUpdatedMediaMetadata();
      if (!newMediaMetadata.equals(mediaMetadata)) {
        mediaMetadata = newMediaMetadata;
        listeners.sendEvent(
            EVENT_MEDIA_METADATA_CHANGED,
            listener -> listener.onMediaMetadataChanged(mediaMetadata));
      }
      // TODO(internal b/187152483): Events should be dispatched via ListenerSet
      for (Listener listener : listenerArraySet) {
        listener.onMetadata(metadata);
      }
    }

    // SurfaceHolder.Callback implementation

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      if (surfaceHolderSurfaceIsVideoOutput) {
        setVideoOutputInternal(holder.getSurface());
      }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      maybeNotifySurfaceSizeChanged(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
      if (surfaceHolderSurfaceIsVideoOutput) {
        setVideoOutputInternal(/* videoOutput= */ null);
      }
      maybeNotifySurfaceSizeChanged(/* width= */ 0, /* height= */ 0);
    }

    // TextureView.SurfaceTextureListener implementation

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
      setSurfaceTextureInternal(surfaceTexture);
      maybeNotifySurfaceSizeChanged(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
      maybeNotifySurfaceSizeChanged(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
      setVideoOutputInternal(/* videoOutput= */ null);
      maybeNotifySurfaceSizeChanged(/* width= */ 0, /* height= */ 0);
      return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
      // Do nothing.
    }

    // SphericalGLSurfaceView.VideoSurfaceListener

    @Override
    public void onVideoSurfaceCreated(Surface surface) {
      setVideoOutputInternal(surface);
    }

    @Override
    public void onVideoSurfaceDestroyed(Surface surface) {
      setVideoOutputInternal(/* videoOutput= */ null);
    }

    // AudioFocusManager.PlayerControl implementation

    @Override
    public void setVolumeMultiplier(float volumeMultiplier) {
      sendVolumeToRenderers();
    }

    @Override
    public void executePlayerCommand(@AudioFocusManager.PlayerCommand int playerCommand) {
      boolean playWhenReady = getPlayWhenReady();
      updatePlayWhenReady(
          playWhenReady, playerCommand, getPlayWhenReadyChangeReason(playWhenReady, playerCommand));
    }

    // AudioBecomingNoisyManager.EventListener implementation.

    @Override
    public void onAudioBecomingNoisy() {
      updatePlayWhenReady(
          /* playWhenReady= */ false,
          AudioFocusManager.PLAYER_COMMAND_DO_NOT_PLAY,
          Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY);
    }

    // StreamVolumeManager.Listener implementation.

    @Override
    public void onStreamTypeChanged(@C.StreamType int streamType) {
      DeviceInfo deviceInfo = createDeviceInfo(streamVolumeManager);
      if (!deviceInfo.equals(ExoPlayerImpl.this.deviceInfo)) {
        ExoPlayerImpl.this.deviceInfo = deviceInfo;
        // TODO(internal b/187152483): Events should be dispatched via ListenerSet
        for (Listener listener : listenerArraySet) {
          listener.onDeviceInfoChanged(deviceInfo);
        }
      }
    }

    @Override
    public void onStreamVolumeChanged(int streamVolume, boolean streamMuted) {
      // TODO(internal b/187152483): Events should be dispatched via ListenerSet
      for (Listener listener : listenerArraySet) {
        listener.onDeviceVolumeChanged(streamVolume, streamMuted);
      }
    }

    // Player.AudioOffloadListener implementation.

    @Override
    public void onExperimentalSleepingForOffloadChanged(boolean sleepingForOffload) {
      updateWakeAndWifiLock();
    }
  }

  /** Listeners that are called on the playback thread. */
  private static final class FrameMetadataListener
      implements VideoFrameMetadataListener, CameraMotionListener, PlayerMessage.Target {

    public static final @MessageType int MSG_SET_VIDEO_FRAME_METADATA_LISTENER =
        Renderer.MSG_SET_VIDEO_FRAME_METADATA_LISTENER;

    public static final @MessageType int MSG_SET_CAMERA_MOTION_LISTENER =
        Renderer.MSG_SET_CAMERA_MOTION_LISTENER;

    public static final @MessageType int MSG_SET_SPHERICAL_SURFACE_VIEW = Renderer.MSG_CUSTOM_BASE;

    @Nullable private VideoFrameMetadataListener videoFrameMetadataListener;
    @Nullable private CameraMotionListener cameraMotionListener;
    @Nullable private VideoFrameMetadataListener internalVideoFrameMetadataListener;
    @Nullable private CameraMotionListener internalCameraMotionListener;

    @Override
    public void handleMessage(@MessageType int messageType, @Nullable Object message) {
      switch (messageType) {
        case MSG_SET_VIDEO_FRAME_METADATA_LISTENER:
          videoFrameMetadataListener = (VideoFrameMetadataListener) message;
          break;
        case MSG_SET_CAMERA_MOTION_LISTENER:
          cameraMotionListener = (CameraMotionListener) message;
          break;
        case MSG_SET_SPHERICAL_SURFACE_VIEW:
          @Nullable SphericalGLSurfaceView surfaceView = (SphericalGLSurfaceView) message;
          if (surfaceView == null) {
            internalVideoFrameMetadataListener = null;
            internalCameraMotionListener = null;
          } else {
            internalVideoFrameMetadataListener = surfaceView.getVideoFrameMetadataListener();
            internalCameraMotionListener = surfaceView.getCameraMotionListener();
          }
          break;
        case Renderer.MSG_SET_AUDIO_ATTRIBUTES:
        case Renderer.MSG_SET_AUDIO_SESSION_ID:
        case Renderer.MSG_SET_AUX_EFFECT_INFO:
        case Renderer.MSG_SET_CHANGE_FRAME_RATE_STRATEGY:
        case Renderer.MSG_SET_SCALING_MODE:
        case Renderer.MSG_SET_SKIP_SILENCE_ENABLED:
        case Renderer.MSG_SET_VIDEO_OUTPUT:
        case Renderer.MSG_SET_VOLUME:
        case Renderer.MSG_SET_WAKEUP_LISTENER:
        default:
          break;
      }
    }

    // VideoFrameMetadataListener

    @Override
    public void onVideoFrameAboutToBeRendered(
        long presentationTimeUs,
        long releaseTimeNs,
        Format format,
        @Nullable MediaFormat mediaFormat) {
      if (internalVideoFrameMetadataListener != null) {
        internalVideoFrameMetadataListener.onVideoFrameAboutToBeRendered(
            presentationTimeUs, releaseTimeNs, format, mediaFormat);
      }
      if (videoFrameMetadataListener != null) {
        videoFrameMetadataListener.onVideoFrameAboutToBeRendered(
            presentationTimeUs, releaseTimeNs, format, mediaFormat);
      }
    }

    // CameraMotionListener

    @Override
    public void onCameraMotion(long timeUs, float[] rotation) {
      if (internalCameraMotionListener != null) {
        internalCameraMotionListener.onCameraMotion(timeUs, rotation);
      }
      if (cameraMotionListener != null) {
        cameraMotionListener.onCameraMotion(timeUs, rotation);
      }
    }

    @Override
    public void onCameraMotionReset() {
      if (internalCameraMotionListener != null) {
        internalCameraMotionListener.onCameraMotionReset();
      }
      if (cameraMotionListener != null) {
        cameraMotionListener.onCameraMotionReset();
      }
    }
  }

  @RequiresApi(31)
  private static final class Api31 {
    private Api31() {}

    @DoNotInline
    public static PlayerId createPlayerId() {
      // TODO: Create a MediaMetricsListener and obtain LogSessionId from it.
      return new PlayerId(LogSessionId.LOG_SESSION_ID_NONE);
    }
  }
}

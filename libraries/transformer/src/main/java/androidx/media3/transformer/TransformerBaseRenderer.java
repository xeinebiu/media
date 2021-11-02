/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.media3.transformer;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.exoplayer.BaseRenderer;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.MediaClock;
import androidx.media3.exoplayer.RendererCapabilities;

@RequiresApi(18)
/* package */ abstract class TransformerBaseRenderer extends BaseRenderer {

  protected final MuxerWrapper muxerWrapper;
  protected final TransformerMediaClock mediaClock;
  protected final Transformation transformation;

  protected boolean isRendererStarted;
  protected long streamOffsetUs;

  public TransformerBaseRenderer(
      int trackType,
      MuxerWrapper muxerWrapper,
      TransformerMediaClock mediaClock,
      Transformation transformation) {
    super(trackType);
    this.muxerWrapper = muxerWrapper;
    this.mediaClock = mediaClock;
    this.transformation = transformation;
  }

  @Override
  protected void onStreamChanged(Format[] formats, long startPositionUs, long offsetUs) {
    this.streamOffsetUs = offsetUs;
  }

  @Override
  @C.FormatSupport
  public final int supportsFormat(Format format) {
    @Nullable String sampleMimeType = format.sampleMimeType;
    if (MimeTypes.getTrackType(sampleMimeType) != getTrackType()) {
      return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE);
    } else if ((MimeTypes.isAudio(sampleMimeType)
            && muxerWrapper.supportsSampleMimeType(
                transformation.audioMimeType == null
                    ? sampleMimeType
                    : transformation.audioMimeType))
        || (MimeTypes.isVideo(sampleMimeType)
            && muxerWrapper.supportsSampleMimeType(
                transformation.videoMimeType == null
                    ? sampleMimeType
                    : transformation.videoMimeType))) {
      return RendererCapabilities.create(C.FORMAT_HANDLED);
    } else {
      return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_SUBTYPE);
    }
  }

  @Override
  public final boolean isReady() {
    return isSourceReady();
  }

  @Override
  public final MediaClock getMediaClock() {
    return mediaClock;
  }

  @Override
  protected final void onEnabled(boolean joining, boolean mayRenderStartOfStream) {
    muxerWrapper.registerTrack();
    mediaClock.updateTimeForTrackType(getTrackType(), 0L);
  }

  @Override
  protected void onStarted() throws ExoPlaybackException {
    isRendererStarted = true;
  }

  @Override
  protected final void onStopped() {
    isRendererStarted = false;
  }
}

/*
 * Copyright 2018 The Android Open Source Project
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

package com.google.android.exoplayer2.session;

import static com.google.android.exoplayer2.session.vct.common.CommonConstants.MOCK_MEDIA2_SESSION_SERVICE;
import static com.google.android.exoplayer2.session.vct.common.TestUtils.SERVICE_CONNECTION_TIMEOUT_MS;
import static com.google.android.exoplayer2.session.vct.common.TestUtils.TIMEOUT_MS;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.content.ComponentName;
import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import com.google.android.exoplayer2.session.vct.common.HandlerThreadTestRule;
import com.google.android.exoplayer2.session.vct.common.TestHandler;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

/** Tests for {@link MediaBrowserCompat} with {@link MediaSessionService}. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaBrowserCompatWithMediaSessionServiceTest {

  private final HandlerThreadTestRule threadTestRule =
      new HandlerThreadTestRule("MediaBrowserCompatTestWithMediaSessionService");
  private final MediaControllerTestRule controllerTestRule =
      new MediaControllerTestRule(threadTestRule);

  @Rule
  public final TestRule chain = RuleChain.outerRule(threadTestRule).around(controllerTestRule);

  Context context;
  TestHandler handler;
  MediaBrowserCompat browserCompat;
  TestConnectionCallback connectionCallback;

  @Before
  public void setUp() throws Exception {
    context = ApplicationProvider.getApplicationContext();
    handler = threadTestRule.getHandler();
    connectionCallback = new TestConnectionCallback();
    handler.postAndSync(
        () -> {
          // Make browser's internal handler to be initialized with test thread.
          browserCompat =
              new MediaBrowserCompat(context, getServiceComponent(), connectionCallback, null);
        });
  }

  @After
  public void cleanUp() {
    if (browserCompat != null) {
      browserCompat.disconnect();
      browserCompat = null;
    }
  }

  ComponentName getServiceComponent() {
    return MOCK_MEDIA2_SESSION_SERVICE;
  }

  void connectAndWait() throws InterruptedException {
    browserCompat.connect();
    assertThat(connectionCallback.connectedLatch.await(SERVICE_CONNECTION_TIMEOUT_MS, MILLISECONDS))
        .isTrue();
  }

  @Test
  public void connect() throws InterruptedException {
    connectAndWait();
    assertThat(connectionCallback.failedLatch.getCount()).isNotEqualTo(0);
  }

  @Ignore
  @Test
  public void connect_rejected() throws InterruptedException {
    // TODO: Connect the browser to the session service whose onConnect() returns null.
    assertThat(connectionCallback.failedLatch.await(TIMEOUT_MS, MILLISECONDS)).isTrue();
    assertThat(connectionCallback.connectedLatch.getCount()).isNotEqualTo(0);
  }

  @Test
  public void getSessionToken() throws Exception {
    connectAndWait();
    MediaControllerCompat controller =
        new MediaControllerCompat(context, browserCompat.getSessionToken());
    assertThat(controller.getPackageName())
        .isEqualTo(browserCompat.getServiceComponent().getPackageName());
  }

  class TestConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
    public final CountDownLatch connectedLatch = new CountDownLatch(1);
    public final CountDownLatch suspendedLatch = new CountDownLatch(1);
    public final CountDownLatch failedLatch = new CountDownLatch(1);

    TestConnectionCallback() {
      super();
    }

    @Override
    public void onConnected() {
      super.onConnected();
      connectedLatch.countDown();
    }

    @Override
    public void onConnectionSuspended() {
      super.onConnectionSuspended();
      suspendedLatch.countDown();
    }

    @Override
    public void onConnectionFailed() {
      super.onConnectionFailed();
      failedLatch.countDown();
    }
  }
}
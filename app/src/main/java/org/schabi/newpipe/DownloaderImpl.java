package org.schabi.newpipe;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.error.ReCaptchaActivity;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.util.InfoCache;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import okhttp3.CompressionInterceptor;
import okhttp3.Gzip;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.brotli.Brotli;

public final class DownloaderImpl extends Downloader {
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
    public static final String YOUTUBE_RESTRICTED_MODE_COOKIE_KEY =
            "youtube_restricted_mode_key";
    public static final String YOUTUBE_RESTRICTED_MODE_COOKIE = "PREF=f2=8000000";
    public static final String YOUTUBE_DOMAIN = "youtube.com";
    /**
     * Prevents YouTube from redirecting HTML requests to consent.youtube.com in regions where
     * the consent page would otherwise loop when fetched by a headless client.
     */
    public static final String YOUTUBE_CONSENT_COOKIE =
            "CONSENT=YES+cb.20210328-17-p0.en+FX";
    /** Current YouTube WEB client version used when the service-worker fetch is bypassed. */
    public static final String YOUTUBE_WEB_CLIENT_VERSION = "2.20260805.01.00";

    private static DownloaderImpl instance;
    private final Map<String, String> mCookies;
    private final OkHttpClient client;

    private DownloaderImpl(final OkHttpClient.Builder builder) {
        this.client = builder
                .readTimeout(30, TimeUnit.SECONDS)
//                .cache(new Cache(new File(context.getExternalCacheDir(), "okhttp"),
//                        16 * 1024 * 1024))
                .addInterceptor(new CompressionInterceptor(
                        Brotli.INSTANCE,
                        Gzip.INSTANCE))
                .build();
        this.mCookies = new HashMap<>();
    }

    @NonNull
    public OkHttpClient getClient() {
        return client;
    }

    /**
     * It's recommended to call exactly once in the entire lifetime of the application.
     *
     * @param builder if null, default builder will be used
     * @return a new instance of {@link DownloaderImpl}
     */
    public static DownloaderImpl init(@Nullable final OkHttpClient.Builder builder) {
        instance = new DownloaderImpl(
                builder != null ? builder : new OkHttpClient.Builder());
        return instance;
    }

    public static DownloaderImpl getInstance() {
        return instance;
    }

    public String getCookies(final String url) {
        final String youtubeCookie = url.contains(YOUTUBE_DOMAIN)
                ? Stream.of(YOUTUBE_CONSENT_COOKIE, getCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; ")) : null;

        // Recaptcha cookie is always added TODO: not sure if this is necessary
        return Stream.of(youtubeCookie, getCookie(ReCaptchaActivity.RECAPTCHA_COOKIES_KEY))
                .filter(Objects::nonNull)
                .flatMap(cookies -> Arrays.stream(cookies.split("; *")))
                .distinct()
                .collect(Collectors.joining("; "));
    }

    public String getCookie(final String key) {
        return mCookies.get(key);
    }

    public void setCookie(final String key, final String cookie) {
        mCookies.put(key, cookie);
    }

    public void removeCookie(final String key) {
        mCookies.remove(key);
    }

    public void updateYoutubeRestrictedModeCookies(final Context context) {
        final String restrictedModeEnabledKey =
                context.getString(R.string.youtube_restricted_mode_enabled);
        final boolean restrictedModeEnabled = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(restrictedModeEnabledKey, false);
        updateYoutubeRestrictedModeCookies(restrictedModeEnabled);
    }

    public void updateYoutubeRestrictedModeCookies(final boolean youtubeRestrictedModeEnabled) {
        if (youtubeRestrictedModeEnabled) {
            setCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY,
                    YOUTUBE_RESTRICTED_MODE_COOKIE);
        } else {
            removeCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY);
        }
        InfoCache.getInstance().clearCache();
    }

    /**
     * Get the size of the content that the url is pointing by firing a HEAD request.
     *
     * @param url an url pointing to the content
     * @return the size of the content, in bytes
     */
    public long getContentLength(final String url) throws IOException {
        try {
            final Response response = head(url);
            return Long.parseLong(response.getHeader("Content-Length"));
        } catch (final NumberFormatException e) {
            throw new IOException("Invalid content length", e);
        } catch (final ReCaptchaException e) {
            throw new IOException(e);
        }
    }

    /**
     * YouTube redirects an empty search-results query between www.youtube.com and m.youtube.com
     * on some Android/device-region combinations. The extractor only needs the page to discover
     * the current client version, so a harmless non-empty query avoids that redirect loop.
     *
     * @param url the original request URL
     * @return the normalized URL
     */
    private static String normalizeYoutubeRequestUrl(final String url) {
        if (url.contains("youtube.com/results?search_query=&")) {
            return url.replace("search_query=&", "search_query=dvid&");
        }
        return url;
    }

    @Override
    public Response execute(@NonNull final Request request)
            throws IOException, ReCaptchaException {
        final String httpMethod = request.httpMethod();
        final String url = normalizeYoutubeRequestUrl(request.url());

        // The extractor normally fetches sw.js to discover this value. On some devices YouTube
        // redirects that fetch into an endless www/mobile loop, so provide the current version
        // locally and prevent the extractor from falling back to the problematic HTML endpoint.
        if (url.equals("https://www.youtube.com/sw.js")) {
            final String serviceWorker = "\"INNERTUBE_CONTEXT_CLIENT_VERSION\":\""
                    + YOUTUBE_WEB_CLIENT_VERSION + "\"";
            return new Response(
                    200,
                    "OK",
                    Map.of("Content-Type", List.of("application/javascript")),
                    serviceWorker,
                    url);
        }

        final Map<String, List<String>> headers = request.headers();
        final byte[] dataToSend = request.dataToSend();

        RequestBody requestBody = null;
        if (dataToSend != null) {
            requestBody = RequestBody.create(dataToSend);
        }

        final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                .method(httpMethod, requestBody)
                .url(url)
                .addHeader("User-Agent", USER_AGENT);

        final String cookies = getCookies(url);
        if (!cookies.isEmpty()) {
            requestBuilder.addHeader("Cookie", cookies);
        }

        final String requestCookies = headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("Cookie"))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.joining("; "));

        headers.forEach((headerName, headerValueList) -> {
            requestBuilder.removeHeader(headerName);
            headerValueList.forEach(headerValue ->
                    requestBuilder.addHeader(headerName, headerValue));
        });

        if (url.contains(YOUTUBE_DOMAIN)) {
            final String mergedCookies = Stream.of(cookies, requestCookies)
                    .filter(cookie -> cookie != null && !cookie.isEmpty())
                    .flatMap(cookie -> Arrays.stream(cookie.split("; *")))
                    .distinct()
                    .collect(Collectors.joining("; "));
            if (!mergedCookies.isEmpty()) {
                requestBuilder.header("Cookie", mergedCookies);
            }
        }

        try (
                okhttp3.Response response = client.newCall(requestBuilder.build()).execute()
        ) {
            if (response.code() == 429) {
                throw new ReCaptchaException("reCaptcha Challenge requested", url);
            }

            String responseBodyToReturn = null;
            try (ResponseBody body = response.body()) {
                responseBodyToReturn = body.string();
            }

            final String latestUrl = response.request().url().toString();
            return new Response(
                    response.code(),
                    response.message(),
                    response.headers().toMultimap(),
                    responseBodyToReturn,
                    latestUrl);
        }
    }
}

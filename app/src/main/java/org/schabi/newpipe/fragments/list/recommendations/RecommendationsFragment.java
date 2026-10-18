package org.schabi.newpipe.fragments.list.recommendations;

import android.os.Bundle;
import android.util.Log;

import org.schabi.newpipe.R;
import org.schabi.newpipe.database.history.model.StreamHistoryEntry;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.fragments.list.BaseListInfoFragment;
import org.schabi.newpipe.local.history.HistoryRecordManager;
import org.schabi.newpipe.util.ExtractorHelper;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.core.Single;

/**
 * A local "For you" feed assembled from YouTube related videos and the user's recent YouTube
 * searches. History remains on-device; only normal extractor requests are made to YouTube.
 */
public class RecommendationsFragment
        extends BaseListInfoFragment<StreamInfoItem, RecommendationsInfo> {
    private static final String TAG = "RecommendationsFragment";
    private static final int MAX_HISTORY_ROWS = 50;
    private static final int MAX_WATCH_SEEDS = 6;
    private static final int MAX_SEARCH_SEEDS = 4;
    private static final int MAX_ITEMS_PER_SEED = 40;
    private static final int MAX_RECOMMENDATIONS = 100;

    private HistoryRecordManager historyRecordManager;

    public RecommendationsFragment() {
        super(UserAction.REQUESTED_STREAM);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyRecordManager = new HistoryRecordManager(requireContext());
        setInitialData(ServiceList.YouTube.getServiceId(), "",
                getString(R.string.recommendations_title));
    }

    @Override
    protected Single<RecommendationsInfo> loadResult(final boolean forceLoad) {
        return Single.fromCallable(() -> {
            final int youtubeServiceId = ServiceList.YouTube.getServiceId();
            final List<StreamHistoryEntry> watchHistory = historyRecordManager
                    .getRecentStreamHistoryForService(youtubeServiceId, MAX_HISTORY_ROWS)
                    .blockingGet();
            final List<String> recentSearches = historyRecordManager
                    .getRecentSearchesForService(youtubeServiceId, MAX_SEARCH_SEEDS)
                    .blockingGet();

            final Set<String> watchedUrls = new HashSet<>();
            for (final StreamHistoryEntry entry : watchHistory) {
                watchedUrls.add(key(entry.getStreamEntity().getServiceId(),
                        entry.getStreamEntity().getUrl()));
            }

            final Map<String, RecommendationCandidate> candidates = new LinkedHashMap<>();
            final int watchSeedCount = Math.min(watchHistory.size(), MAX_WATCH_SEEDS);
            for (int i = 0; i < watchSeedCount; i++) {
                final StreamHistoryEntry watched = watchHistory.get(i);
                try {
                    final StreamInfo info = ExtractorHelper.getStreamInfo(
                            youtubeServiceId, watched.getStreamEntity().getUrl(), forceLoad)
                            .blockingGet();
                    addCandidates(candidates, info.getRelatedItems(), 5.0 / (i + 1), watchedUrls);
                } catch (final Exception exception) {
                    Log.d(TAG, "Skipping a watch-history recommendation seed", exception);
                }
            }

            for (int i = 0; i < recentSearches.size(); i++) {
                final String query = recentSearches.get(i);
                try {
                    final SearchInfo searchInfo = ExtractorHelper.searchFor(
                            youtubeServiceId, query, Collections.emptyList(), null).blockingGet();
                    addCandidates(candidates, searchInfo.getRelatedItems(),
                            3.0 / (i + 1), watchedUrls);
                } catch (final Exception exception) {
                    Log.d(TAG, "Skipping a search-history recommendation seed", exception);
                }
            }

            final List<StreamInfoItem> rankedItems = candidates.values().stream()
                    .sorted(Comparator.comparingDouble(RecommendationCandidate::getScore)
                            .reversed())
                    .limit(MAX_RECOMMENDATIONS)
                    .map(RecommendationCandidate::getItem)
                    .toList();
            return new RecommendationsInfo(rankedItems, name);
        });
    }

    @Override
    protected Single<ListExtractor.InfoItemsPage<StreamInfoItem>> loadMoreItemsLogic() {
        return Single.just(ListExtractor.InfoItemsPage.emptyPage());
    }

    @Override
    public void showEmptyState() {
        setEmptyStateMessage(R.string.recommendations_empty);
        super.showEmptyState();
    }

    private static void addCandidates(
            final Map<String, RecommendationCandidate> candidates,
            final List<? extends InfoItem> items,
            final double sourceWeight,
            final Set<String> watchedUrls) {
        if (items == null) {
            return;
        }

        int rank = 0;
        for (final InfoItem item : items) {
            if (!(item instanceof StreamInfoItem streamItem)
                    || streamItem.getServiceId() != ServiceList.YouTube.getServiceId()) {
                continue;
            }

            final String candidateKey = key(streamItem.getServiceId(), streamItem.getUrl());
            if (watchedUrls.contains(candidateKey)) {
                continue;
            }

            final double score = sourceWeight / Math.sqrt(rank + 1);
            final RecommendationCandidate existing = candidates.get(candidateKey);
            if (existing == null) {
                candidates.put(candidateKey, new RecommendationCandidate(streamItem, score));
            } else {
                existing.addScore(score);
            }
            rank++;
            if (rank >= MAX_ITEMS_PER_SEED) {
                break;
            }
        }
    }

    private static String key(final int serviceId, final String url) {
        return serviceId + ":" + url;
    }

    private static final class RecommendationCandidate {
        private final StreamInfoItem item;
        private double score;

        RecommendationCandidate(final StreamInfoItem item, final double score) {
            this.item = item;
            this.score = score;
        }

        void addScore(final double extraScore) {
            score += extraScore;
        }

        double getScore() {
            return score;
        }

        StreamInfoItem getItem() {
            return item;
        }
    }
}

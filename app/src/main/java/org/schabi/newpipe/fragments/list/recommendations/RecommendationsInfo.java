package org.schabi.newpipe.fragments.list.recommendations;

import org.schabi.newpipe.extractor.ListInfo;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.Collections;
import java.util.List;

/** List wrapper for videos ranked from local watch and search history. */
public final class RecommendationsInfo extends ListInfo<StreamInfoItem> {
    public RecommendationsInfo(final List<StreamInfoItem> recommendations,
                               final String title) {
        super(ServiceList.YouTube.getServiceId(), "local-recommendations", "", "", title,
                Collections.emptyList(), null);
        setRelatedItems(recommendations);
    }
}

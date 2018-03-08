package com.battlelancer.seriesguide.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.provider.ProviderTestRule;
import android.support.test.runner.AndroidJUnit4;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.util.DBUtils;
import java.util.ArrayList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProviderTest {

    private static final Show SHOW;

    static {
        SHOW = new Show();
        SHOW.tvdb_id = 12;
    }

    @Rule
    public ProviderTestRule providerRule = new ProviderTestRule.Builder(SeriesGuideProvider.class,
            SgApp.CONTENT_AUTHORITY).build();

    @Test
    public void showDefaultValues() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        ContentValues values = SHOW.toContentValues(context, true);
        ContentProviderOperation op = ContentProviderOperation.newInsert(Shows.CONTENT_URI)
                .withValues(values).build();

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        batch.add(op);
        providerRule.getResolver().applyBatch(SgApp.CONTENT_AUTHORITY, batch);

        Cursor query = providerRule.getResolver().query(Shows.CONTENT_URI, null,
                null, null, null);
        assertNotNull(query);
        assertTrue(query.moveToFirst());

        assertEquals(SHOW.tvdb_id, query.getInt(query.getColumnIndexOrThrow(Shows._ID)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.TITLE)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.OVERVIEW)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.GENRES)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.NETWORK)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.RUNTIME)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.STATUS)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.CONTENTRATING)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.NEXTEPISODE)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.POSTER)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.NEXTTEXT)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.IMDBID)));
        // getInt returns 0 if NULL, so check explicitly
        assertDefaultValue(query, Shows.TRAKT_ID, 0);
        assertDefaultValue(query, Shows.FAVORITE, 0);
        assertDefaultValue(query, Shows.HEXAGON_MERGE_COMPLETE, 1);
        assertDefaultValue(query, Shows.HIDDEN, 0);
        assertDefaultValue(query, Shows.LASTUPDATED, 0);
        assertDefaultValue(query, Shows.LASTEDIT, 0);
        assertDefaultValue(query, Shows.LASTWATCHEDID, 0);
        assertDefaultValue(query, Shows.LASTWATCHED_MS, 0);
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Shows.LANGUAGE)));
        assertDefaultValue(query, Shows.UNWATCHED_COUNT, DBUtils.UNKNOWN_UNWATCHED_COUNT);
        assertDefaultValue(query, Shows.NOTIFY, 1);

        assertEquals(1, query.getCount());

        query.close();
    }

    @Test
    public void seasonDefaultValues() throws Exception {
        ContentProviderOperation op = DBUtils
                .buildSeasonOp(12, 1234, 42, true);

        // note how this does not cause a foreign key constraint failure
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        batch.add(op);
        providerRule.getResolver().applyBatch(SgApp.CONTENT_AUTHORITY, batch);

        Cursor query = providerRule.getResolver().query(Seasons.CONTENT_URI, null,
                null, null, null);
        assertNotNull(query);
        assertTrue(query.moveToFirst());

        assertEquals(1234, query.getInt(query.getColumnIndexOrThrow(Seasons._ID)));
        assertEquals(12, query.getInt(query.getColumnIndexOrThrow(Shows.REF_SHOW_ID)));
        assertEquals(42, query.getInt(query.getColumnIndexOrThrow(Seasons.COMBINED)));
        // getInt returns 0 if NULL, so check explicitly
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Seasons.WATCHCOUNT)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Seasons.UNAIREDCOUNT)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Seasons.NOAIRDATECOUNT)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Seasons.TOTALCOUNT)));
        assertEquals(0, query.getInt(query.getColumnIndexOrThrow(Seasons.WATCHCOUNT)));
        assertEquals(0, query.getInt(query.getColumnIndexOrThrow(Seasons.UNAIREDCOUNT)));
        assertEquals(0, query.getInt(query.getColumnIndexOrThrow(Seasons.NOAIRDATECOUNT)));
        assertEquals(0, query.getInt(query.getColumnIndexOrThrow(Seasons.TOTALCOUNT)));

        assertEquals(1, query.getCount());

        query.close();
    }

    private void assertDefaultValue(Cursor query, String column, int defaultValue) {
        assertFalse(query.isNull(query.getColumnIndexOrThrow(column)));
        assertEquals(defaultValue, query.getInt(query.getColumnIndexOrThrow(column)));
    }
}
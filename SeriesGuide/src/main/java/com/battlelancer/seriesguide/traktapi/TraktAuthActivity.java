package com.battlelancer.seriesguide.traktapi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.enums.TraktResult;
import com.battlelancer.seriesguide.ui.BaseOAuthActivity;
import com.battlelancer.seriesguide.util.ConnectTraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.trakt5.TraktV2;
import dagger.Lazy;
import java.math.BigInteger;
import java.security.SecureRandom;
import javax.inject.Inject;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * Starts a trakt OAuth 2.0 authorization flow using the default browser or an embedded {@link
 * android.webkit.WebView} as a fallback.
 */
public class TraktAuthActivity extends BaseOAuthActivity {

    private static final String KEY_STATE = "state";
    private static final String TRAKT_CONNECT_TASK_TAG = "trakt-connect-task";
    private String state;
    private ConnectTraktTaskFragment taskFragment;
    @Inject Lazy<TraktV2> trakt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SgApp.from(this).getServicesComponent().inject(this);
        super.onCreate(savedInstanceState);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TRAKT_CONNECT_TASK_TAG);
        if (fragment != null) {
            taskFragment = (ConnectTraktTaskFragment) fragment;
        }

        if (savedInstanceState != null) {
            // restore state on recreation
            // (e.g. if launching external browser dropped us out of memory)
            state = savedInstanceState.getString(KEY_STATE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_STATE, state);
    }

    @Override
    @Nullable
    protected String getAuthorizationUrl() {
        state = new BigInteger(130, new SecureRandom()).toString(32);
        try {
            OAuthClientRequest request = trakt.get().buildAuthorizationRequest(state);
            return request.getLocationUri();
        } catch (OAuthSystemException e) {
            Timber.e(e, "Building auth request failed.");
            activateFallbackButtons();
            setMessage(getAuthErrorMessage() + "\n\n(" + e.getMessage() + ")");
        }

        return null;
    }

    @Override
    protected String getAuthErrorMessage() {
        return getString(R.string.trakt_error_credentials);
    }

    @Override
    protected void fetchTokensAndFinish(@Nullable String authCode, @Nullable String state) {
        activateFallbackButtons();

        if (taskFragment == null) {
            taskFragment = new ConnectTraktTaskFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(taskFragment, TRAKT_CONNECT_TASK_TAG)
                    .commit();
        }

        if (taskFragment.getTask() != null
                && taskFragment.getTask().getStatus() != AsyncTask.Status.FINISHED) {
            // connect task is still running
            setMessage(getString(R.string.waitplease), true);
            return;
        }

        // if state does not match what we sent, drop the auth code
        if (this.state == null || !this.state.equals(state)) {
            Timber.e(OAuthProblemException.error("invalid_state",
                    "State is null or does not match."), "fetchTokensAndFinish: failed.");
            setMessage(getAuthErrorMessage() + (this.state == null ?
                    "\n\n(State is null.)" :
                    "\n\n(State does not match. Cross-site request forgery detected.)"));
            return;
        }

        if (TextUtils.isEmpty(authCode)) {
            // no valid auth code, remain in activity and show fallback buttons
            setMessage(getAuthErrorMessage() + "\n\n(No auth code returned.)");
            return;
        }

        // fetch access token with given OAuth auth code
        setMessage(getString(R.string.waitplease), true);
        ConnectTraktTask task = new ConnectTraktTask(SgApp.from(this));
        Utils.executeInOrder(task, authCode);
        taskFragment.setTask(task);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ConnectTraktTask.FinishedEvent event) {
        taskFragment.setTask(null);

        int resultCode = event.resultCode;
        if (resultCode == TraktResult.SUCCESS) {
            // if we got here, looks like credentials were stored successfully
            finish();
            return;
        }

        // handle errors
        String errorText;
        switch (resultCode) {
            case TraktResult.OFFLINE:
                errorText = getString(R.string.offline);
                break;
            case TraktResult.API_ERROR:
                errorText = getString(R.string.api_error_generic, getString(R.string.trakt));
                break;
            case TraktResult.AUTH_ERROR:
            case TraktResult.ERROR:
            default:
                errorText = getString(R.string.trakt_error_credentials);
                break;
        }
        setMessage(errorText);
        activateFallbackButtons();
    }
}

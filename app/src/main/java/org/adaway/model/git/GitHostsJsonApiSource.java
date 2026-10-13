package org.adaway.model.git;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

/**
 * This class is an utility class to get information from Git hosted hosts sources from JSON API.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public abstract class GitHostsJsonApiSource extends GitHostsSource {
    /**
     * The HTTP client, shared by all the requests as OkHttp recommends.
     */
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    @Override
    @Nullable
    public ZonedDateTime getLastUpdate() {
        return getLastUpdateFromApi(getCommitApiUrl());
    }

    protected abstract String getCommitApiUrl();

    @Nullable
    protected ZonedDateTime getLastUpdateFromApi(String commitApiUrl) {
        Request request = new Request.Builder().url(commitApiUrl).build();
        try (Response response = HTTP_CLIENT.newCall(request).execute();
             ResponseBody body = response.body()) {
            if (response.isSuccessful()) {
                return parseJsonBody(body.string());
            }
        } catch (UnknownHostException | SocketTimeoutException exception) {
            Timber.i(exception, "Unable to reach API backend.");
        } catch (IOException | JSONException exception) {
            Timber.e(exception, "Unable to get commits from API.");
        }
        // Return failed
        return null;
    }

    /**
     * Get the last update date from the API response body.
     *
     * @param body The API response body.
     * @return The last update date, {@code null} if it could not be read.
     * @throws JSONException If the body is not the expected JSON.
     */
    @Nullable
    protected abstract ZonedDateTime parseJsonBody(String body) throws JSONException;

    /**
     * Get the date of the most recent commit from a commit list returned by an API.
     *
     * @param body     The commit list, most recent first.
     * @param datePath The JSON object keys leading to the date of a commit.
     * @return The date of the most recent commit with a readable date, {@code null} if none.
     * @throws JSONException If the body is not a commit list.
     */
    @Nullable
    protected static ZonedDateTime parseCommitList(String body, String... datePath) throws JSONException {
        JSONArray commits = new JSONArray(body);
        for (int i = 0; i < commits.length(); i++) {
            JSONObject object = commits.getJSONObject(i);
            for (int key = 0; key < datePath.length - 1; key++) {
                object = object.getJSONObject(datePath[key]);
            }
            ZonedDateTime date = parseDate(object.getString(datePath[datePath.length - 1]));
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    /**
     * Parse an ISO-8601 date returned by an API.
     *
     * @param dateString The date to parse.
     * @return The parsed date, {@code null} if it could not be parsed.
     */
    @Nullable
    protected static ZonedDateTime parseDate(String dateString) {
        try {
            return ZonedDateTime.parse(dateString);
        } catch (DateTimeParseException exception) {
            Timber.w(exception, "Failed to parse commit date: %s.", dateString);
            return null;
        }
    }
}

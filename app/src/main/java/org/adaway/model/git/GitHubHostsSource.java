package org.adaway.model.git;

import static java.util.stream.Collectors.joining;

import androidx.annotation.Nullable;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Arrays;

/**
 * This class is an utility class to get information from GitHub repository hosting.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
class GitHubHostsSource extends GitHostsJsonApiSource {
    /**
     * The GitHub owner name.
     */
    private final String owner;
    /**
     * The GitHub repository name.
     */
    private final String repo;
    /**
     * The GitHub blob (hosts file) path.
     */
    private final String blobPath;

    /**
     * Constructor.
     *
     * @param url The hosts file URL hosted on GitHub.
     * @throws MalformedURLException If the URl is not a GitHub URL.
     */
    GitHubHostsSource(String url) throws MalformedURLException {
        // Check URL path
        URL parsedUrl = new URL(url);
        String path = parsedUrl.getPath();
        String[] pathParts = path.split("/");
        if (pathParts.length < 5) {
            throw new MalformedURLException("The GitHub user content URL " + url + " is not valid.");
        }
        // Extract components from path
        this.owner = pathParts[1];
        this.repo = pathParts[2];
        this.blobPath = Arrays.stream(pathParts)
                .skip(4)
                .collect(joining("/"));
    }

    @Override
    protected String getCommitApiUrl() {
        return "https://api.github.com/repos/" + this.owner + "/" + this.repo +
                "/commits?per_page=1&path=" + this.blobPath;
    }

    @Nullable
    @Override
    protected ZonedDateTime parseJsonBody(String body) throws JSONException {
        return parseCommitList(body, "commit", "committer", "date");
    }
}

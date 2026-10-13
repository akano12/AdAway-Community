package org.adaway.model.backup;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.UiThread;

import org.adaway.R;
import org.adaway.db.AppDatabase;
import org.adaway.db.dao.HostListItemDao;
import org.adaway.db.dao.HostsSourceDao;
import org.adaway.db.entity.HostListItem;
import org.adaway.db.entity.HostsSource;
import org.adaway.db.entity.ListType;
import org.adaway.util.AppExecutors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import static org.adaway.db.entity.ListType.ALLOWED;
import static org.adaway.db.entity.ListType.BLOCKED;
import static org.adaway.db.entity.ListType.REDIRECTED;
import static org.adaway.model.backup.BackupFormat.ALLOWED_KEY;
import static org.adaway.model.backup.BackupFormat.BLOCKED_KEY;
import static org.adaway.model.backup.BackupFormat.REDIRECTED_KEY;
import static org.adaway.model.backup.BackupFormat.SOURCES_KEY;
import static org.adaway.model.backup.BackupFormat.hostFromJson;
import static org.adaway.model.backup.BackupFormat.sourceFromJson;

import timber.log.Timber;

/**
 * This class is a helper class to import user lists and hosts sources to a backup file.<br>
 * Importing a file source will no restore read access from Storage Access Framework.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public final class BackupImporter {

    private BackupImporter() {

    }

    private static final Executor DISK_IO_EXECUTOR = AppExecutors.getInstance().diskIO();
    private static final Executor MAIN_THREAD_EXECUTOR = AppExecutors.getInstance().mainThread();

    /**
     * Import a backup file.
     *
     * @param context   The application context.
     * @param backupUri The URI of a backup file.
     */
    @UiThread
    public static void importFromBackup(Context context, Uri backupUri) {
        DISK_IO_EXECUTOR.execute(() -> {
            boolean imported = true;
            try {
                importBackup(context, backupUri);
            } catch (IOException e) {
                Timber.e(e, "Failed to import backup.");
                imported = false;
            }
            boolean successful = imported;
            MAIN_THREAD_EXECUTOR.execute(() -> notifyImportEnd(context, successful));
        });
    }

    @UiThread
    private static void notifyImportEnd(Context context, boolean successful) {
        Toast.makeText(
                context,
                context.getString(successful ? R.string.import_success : R.string.import_failed),
                Toast.LENGTH_LONG
        ).show();
    }

    static void importBackup(Context context, Uri backupUri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(backupUri)) {
            if (inputStream == null) {
                throw new IOException("Failed to open backup file.");
            }
            importBackup(context, inputStream);
        } catch (FileNotFoundException exception) {
            throw new IOException("Failed to find backup file.", exception);
        }
    }

    private static void importBackup(Context context, InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }
            JSONObject backupObject = new JSONObject(contentBuilder.toString());
            importBackup(context, backupObject);
        } catch (JSONException exception) {
            throw new IOException("Failed to parse backup file.", exception);
        }
    }

    private static void importBackup(Context context, JSONObject backupObject) throws JSONException {
        AppDatabase database = AppDatabase.getInstance(context);
        HostsSourceDao hostsSourceDao = database.hostsSourceDao();
        HostListItemDao hostListItemDao = database.hostsListItemDao();
        JSONArray sources = backupObject.getJSONArray(SOURCES_KEY);
        JSONArray blocked = backupObject.getJSONArray(BLOCKED_KEY);
        JSONArray allowed = backupObject.getJSONArray(ALLOWED_KEY);
        JSONArray redirected = backupObject.getJSONArray(REDIRECTED_KEY);
        // Restore all or nothing, rather than leave a partly restored backup
        database.runInTransaction(() -> {
            importSourceBackup(hostsSourceDao, sources);
            importListBackup(hostListItemDao, BLOCKED, blocked);
            importListBackup(hostListItemDao, ALLOWED, allowed);
            importListBackup(hostListItemDao, REDIRECTED, redirected);
        });
    }

    private static void importSourceBackup(HostsSourceDao hostsSourceDao, JSONArray sources) {
        for (int index = 0; index < sources.length(); index++) {
            HostsSource source;
            try {
                source = sourceFromJson(sources.getJSONObject(index));
            } catch (JSONException e) {
                // Skip the entry rather than abandoning the whole restore: one rejected value
                // must not cost the user every other entry in their backup.
                Timber.w(e, "Skipping invalid source entry %d.", index);
                continue;
            }
            hostsSourceDao.insert(source);
        }
    }

    private static void importListBackup(HostListItemDao hostListItemDao, ListType type, JSONArray hosts) {
        for (int index = 0; index < hosts.length(); index++) {
            HostListItem host;
            try {
                host = hostFromJson(hosts.getJSONObject(index));
            } catch (JSONException e) {
                Timber.w(e, "Skipping invalid %s entry %d.", type, index);
                continue;
            }
            host.setType(type);
            hostListItemDao.upsertUserItem(host);
        }
    }
}

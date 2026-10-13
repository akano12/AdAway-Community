package org.adaway.tile;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import org.adaway.AdAwayApplication;
import org.adaway.model.adblocking.AdBlockModel;
import org.adaway.model.error.HostErrorException;
import org.adaway.util.AppExecutors;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.service.quicksettings.Tile.STATE_ACTIVE;
import static android.service.quicksettings.Tile.STATE_INACTIVE;

import timber.log.Timber;

/**
 * This class is a {@link TileService} to toggle ad-blocking.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class AdBlockingTileService extends TileService {
    private final AtomicBoolean toggling = new AtomicBoolean(false);
    // Single Observer instance so onStopListening removes exactly what onStartListening registered.
    // A "this::updateTile" method reference creates a new instance each time, so removeObserver
    // would never match and observers would pile up.
    private final Observer<Boolean> appliedObserver = applied -> updateTile(Boolean.TRUE.equals(applied));
    /**
     * The applied state observed while listening: the model changes with the ad blocking method.
     */
    private LiveData<Boolean> observedApplied;

    @Override
    public void onTileAdded() {
        boolean adBlocked = getModel().isApplied().getValue() == Boolean.TRUE;
        updateTile(adBlocked);
    }

    @Override
    public void onStartListening() {
        this.observedApplied = getModel().isApplied();
        this.observedApplied.observeForever(this.appliedObserver);
    }

    @Override
    public void onStopListening() {
        if (this.observedApplied != null) {
            this.observedApplied.removeObserver(this.appliedObserver);
            this.observedApplied = null;
        }
    }

    @Override
    public void onClick() {
        AppExecutors.getInstance()
                .diskIO()
                .execute(this::toggleAdBlocking);
    }

    private void updateTile(boolean adBlocked) {
        Tile tile = getQsTile();
        // The tile is null when the system has unbound the service (it may happen between
        // a click that triggered an async toggle and the toggle completing).
        if (tile == null) {
            return;
        }
        tile.setState(adBlocked ? STATE_ACTIVE : STATE_INACTIVE);
        tile.updateTile();
    }

    private void toggleAdBlocking() {
        if (!this.toggling.compareAndSet(false, true)) {
            return;
        }
        AdBlockModel model = getModel();
        boolean wasApplied = model.isApplied().getValue() == Boolean.TRUE;
        try {
            Timber.i("Tile: user toggle (currently %s).", wasApplied ? "ACTIVE" : "INACTIVE");
            if (wasApplied) {
                model.revert();
            } else {
                model.apply();
            }
        } catch (HostErrorException e) {
            Timber.w(e, "Failed to toggle ad-blocking.");
        } finally {
            this.toggling.set(false);
            // Whatever happened (success, failure, exception), force the tile back in
            // sync with the LiveData so a failed start doesn't leave the tile stuck on
            // an optimistic "ACTIVE" state.
            updateTile(model.isApplied().getValue() == Boolean.TRUE);
        }
    }

    private AdBlockModel getModel() {
        return ((AdAwayApplication) getApplication()).getAdBlockModel();
    }
}

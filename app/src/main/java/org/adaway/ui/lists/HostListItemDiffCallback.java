package org.adaway.ui.lists;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import org.adaway.db.entity.HostListItem;

/**
 * This class compares the host list items of the mobile and TV list screens.
 */
public final class HostListItemDiffCallback extends DiffUtil.ItemCallback<HostListItem> {
    /**
     * The shared instance: the callback has no state.
     */
    public static final HostListItemDiffCallback INSTANCE = new HostListItemDiffCallback();

    private HostListItemDiffCallback() {

    }

    @Override
    public boolean areItemsTheSame(@NonNull HostListItem oldItem, @NonNull HostListItem newItem) {
        return oldItem.getHost().equals(newItem.getHost());
    }

    @Override
    public boolean areContentsTheSame(@NonNull HostListItem oldItem, @NonNull HostListItem newItem) {
        return oldItem.equals(newItem);
    }
}

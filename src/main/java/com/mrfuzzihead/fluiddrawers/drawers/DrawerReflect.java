package com.mrfuzzihead.fluiddrawers.drawers;

import java.lang.reflect.Field;
import java.util.Map;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityController;

/**
 * Reflection utility to access the private {@code storage} field of
 * {@link TileEntityController} and the {@code storage} field of its inner
 * {@code StorageRecord} class.
 *
 * <p>Replaces the 1.12.2 version's {@code libnine MirrorUtils} approach
 * with plain Java reflection (1.7.10 compatible).</p>
 */
public class DrawerReflect {

    private static final Class<?> CLASS_STORAGE_RECORD;
    private static final Field FIELD_CONTROLLER_STORAGE;
    private static final Field FIELD_RECORD_STORAGE;

    static {
        try {
            // Access TileEntityController's private 'storage' field
            // The field type is: Map<BlockCoord, StorageRecord>
            FIELD_CONTROLLER_STORAGE = TileEntityController.class.getDeclaredField("storage");
            FIELD_CONTROLLER_STORAGE.setAccessible(true);

            // Access the inner class StorageRecord's 'storage' field (IDrawerGroup)
            CLASS_STORAGE_RECORD = Class.forName(
                "com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityController$StorageRecord");
            FIELD_RECORD_STORAGE = CLASS_STORAGE_RECORD.getDeclaredField("storage");
            FIELD_RECORD_STORAGE.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize DrawerReflect for Storage Drawers reflection!", e);
        }
    }

    /**
     * Returns the controller's internal storage map.
     *
     * <p>In SD 1.7.10, the field is declared as:
     * <pre>{@code private Map<BlockCoord, StorageRecord> storage;}</pre>
     *
     * @param controller The controller instance
     * @return The storage map (BlockCoord -> StorageRecord)
     */
    @SuppressWarnings("unchecked")
    public static Map<?, ?> getStorageRecords(TileEntityController controller) {
        try {
            return (Map<?, ?>) FIELD_CONTROLLER_STORAGE.get(controller);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access controller storage field", e);
        }
    }

    /**
     * Returns the {@link IDrawerGroup} from a storage record.
     *
     * <p>In SD 1.7.10, the StorageRecord field is:
     * <pre>{@code public IDrawerGroup storage;}</pre>
     *
     * @param storageRecord A StorageRecord instance from the controller's storage map
     * @return The IDrawerGroup stored in the record, or null
     */
    public static IDrawerGroup getGroupForRecord(Object storageRecord) {
        try {
            return (IDrawerGroup) FIELD_RECORD_STORAGE.get(storageRecord);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access StorageRecord.storage field", e);
        }
    }
}

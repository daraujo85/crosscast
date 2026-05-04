package com.campilot.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PresetDao_Impl implements PresetDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Preset> __insertionAdapterOfPreset;

  private final EntityDeletionOrUpdateAdapter<Preset> __deletionAdapterOfPreset;

  private final EntityDeletionOrUpdateAdapter<Preset> __updateAdapterOfPreset;

  public PresetDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPreset = new EntityInsertionAdapter<Preset>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `presets` (`id`,`name`,`cameraId`,`zoomRatio`,`focusMode`,`exposureLock`,`torch`,`resolution`,`fps`,`switchDelayMs`,`settleTimeMs`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Preset entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getCameraId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getCameraId());
        }
        statement.bindDouble(4, entity.getZoomRatio());
        if (entity.getFocusMode() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getFocusMode());
        }
        final int _tmp = entity.getExposureLock() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.getTorch() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        if (entity.getResolution() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getResolution());
        }
        statement.bindLong(9, entity.getFps());
        statement.bindLong(10, entity.getSwitchDelayMs());
        statement.bindLong(11, entity.getSettleTimeMs());
      }
    };
    this.__deletionAdapterOfPreset = new EntityDeletionOrUpdateAdapter<Preset>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `presets` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Preset entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
      }
    };
    this.__updateAdapterOfPreset = new EntityDeletionOrUpdateAdapter<Preset>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `presets` SET `id` = ?,`name` = ?,`cameraId` = ?,`zoomRatio` = ?,`focusMode` = ?,`exposureLock` = ?,`torch` = ?,`resolution` = ?,`fps` = ?,`switchDelayMs` = ?,`settleTimeMs` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Preset entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getCameraId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getCameraId());
        }
        statement.bindDouble(4, entity.getZoomRatio());
        if (entity.getFocusMode() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getFocusMode());
        }
        final int _tmp = entity.getExposureLock() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.getTorch() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        if (entity.getResolution() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getResolution());
        }
        statement.bindLong(9, entity.getFps());
        statement.bindLong(10, entity.getSwitchDelayMs());
        statement.bindLong(11, entity.getSettleTimeMs());
        if (entity.getId() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getId());
        }
      }
    };
  }

  @Override
  public Object insertPreset(final Preset preset, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPreset.insert(preset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePreset(final Preset preset, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPreset.handle(preset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePreset(final Preset preset, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPreset.handle(preset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Preset>> getAllPresets() {
    final String _sql = "SELECT * FROM presets";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"presets"}, new Callable<List<Preset>>() {
      @Override
      @NonNull
      public List<Preset> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCameraId = CursorUtil.getColumnIndexOrThrow(_cursor, "cameraId");
          final int _cursorIndexOfZoomRatio = CursorUtil.getColumnIndexOrThrow(_cursor, "zoomRatio");
          final int _cursorIndexOfFocusMode = CursorUtil.getColumnIndexOrThrow(_cursor, "focusMode");
          final int _cursorIndexOfExposureLock = CursorUtil.getColumnIndexOrThrow(_cursor, "exposureLock");
          final int _cursorIndexOfTorch = CursorUtil.getColumnIndexOrThrow(_cursor, "torch");
          final int _cursorIndexOfResolution = CursorUtil.getColumnIndexOrThrow(_cursor, "resolution");
          final int _cursorIndexOfFps = CursorUtil.getColumnIndexOrThrow(_cursor, "fps");
          final int _cursorIndexOfSwitchDelayMs = CursorUtil.getColumnIndexOrThrow(_cursor, "switchDelayMs");
          final int _cursorIndexOfSettleTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "settleTimeMs");
          final List<Preset> _result = new ArrayList<Preset>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Preset _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpCameraId;
            if (_cursor.isNull(_cursorIndexOfCameraId)) {
              _tmpCameraId = null;
            } else {
              _tmpCameraId = _cursor.getString(_cursorIndexOfCameraId);
            }
            final float _tmpZoomRatio;
            _tmpZoomRatio = _cursor.getFloat(_cursorIndexOfZoomRatio);
            final String _tmpFocusMode;
            if (_cursor.isNull(_cursorIndexOfFocusMode)) {
              _tmpFocusMode = null;
            } else {
              _tmpFocusMode = _cursor.getString(_cursorIndexOfFocusMode);
            }
            final boolean _tmpExposureLock;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfExposureLock);
            _tmpExposureLock = _tmp != 0;
            final boolean _tmpTorch;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfTorch);
            _tmpTorch = _tmp_1 != 0;
            final String _tmpResolution;
            if (_cursor.isNull(_cursorIndexOfResolution)) {
              _tmpResolution = null;
            } else {
              _tmpResolution = _cursor.getString(_cursorIndexOfResolution);
            }
            final int _tmpFps;
            _tmpFps = _cursor.getInt(_cursorIndexOfFps);
            final long _tmpSwitchDelayMs;
            _tmpSwitchDelayMs = _cursor.getLong(_cursorIndexOfSwitchDelayMs);
            final long _tmpSettleTimeMs;
            _tmpSettleTimeMs = _cursor.getLong(_cursorIndexOfSettleTimeMs);
            _item = new Preset(_tmpId,_tmpName,_tmpCameraId,_tmpZoomRatio,_tmpFocusMode,_tmpExposureLock,_tmpTorch,_tmpResolution,_tmpFps,_tmpSwitchDelayMs,_tmpSettleTimeMs);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getPresetById(final String id, final Continuation<? super Preset> $completion) {
    final String _sql = "SELECT * FROM presets WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Preset>() {
      @Override
      @Nullable
      public Preset call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCameraId = CursorUtil.getColumnIndexOrThrow(_cursor, "cameraId");
          final int _cursorIndexOfZoomRatio = CursorUtil.getColumnIndexOrThrow(_cursor, "zoomRatio");
          final int _cursorIndexOfFocusMode = CursorUtil.getColumnIndexOrThrow(_cursor, "focusMode");
          final int _cursorIndexOfExposureLock = CursorUtil.getColumnIndexOrThrow(_cursor, "exposureLock");
          final int _cursorIndexOfTorch = CursorUtil.getColumnIndexOrThrow(_cursor, "torch");
          final int _cursorIndexOfResolution = CursorUtil.getColumnIndexOrThrow(_cursor, "resolution");
          final int _cursorIndexOfFps = CursorUtil.getColumnIndexOrThrow(_cursor, "fps");
          final int _cursorIndexOfSwitchDelayMs = CursorUtil.getColumnIndexOrThrow(_cursor, "switchDelayMs");
          final int _cursorIndexOfSettleTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "settleTimeMs");
          final Preset _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpCameraId;
            if (_cursor.isNull(_cursorIndexOfCameraId)) {
              _tmpCameraId = null;
            } else {
              _tmpCameraId = _cursor.getString(_cursorIndexOfCameraId);
            }
            final float _tmpZoomRatio;
            _tmpZoomRatio = _cursor.getFloat(_cursorIndexOfZoomRatio);
            final String _tmpFocusMode;
            if (_cursor.isNull(_cursorIndexOfFocusMode)) {
              _tmpFocusMode = null;
            } else {
              _tmpFocusMode = _cursor.getString(_cursorIndexOfFocusMode);
            }
            final boolean _tmpExposureLock;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfExposureLock);
            _tmpExposureLock = _tmp != 0;
            final boolean _tmpTorch;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfTorch);
            _tmpTorch = _tmp_1 != 0;
            final String _tmpResolution;
            if (_cursor.isNull(_cursorIndexOfResolution)) {
              _tmpResolution = null;
            } else {
              _tmpResolution = _cursor.getString(_cursorIndexOfResolution);
            }
            final int _tmpFps;
            _tmpFps = _cursor.getInt(_cursorIndexOfFps);
            final long _tmpSwitchDelayMs;
            _tmpSwitchDelayMs = _cursor.getLong(_cursorIndexOfSwitchDelayMs);
            final long _tmpSettleTimeMs;
            _tmpSettleTimeMs = _cursor.getLong(_cursorIndexOfSettleTimeMs);
            _result = new Preset(_tmpId,_tmpName,_tmpCameraId,_tmpZoomRatio,_tmpFocusMode,_tmpExposureLock,_tmpTorch,_tmpResolution,_tmpFps,_tmpSwitchDelayMs,_tmpSettleTimeMs);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

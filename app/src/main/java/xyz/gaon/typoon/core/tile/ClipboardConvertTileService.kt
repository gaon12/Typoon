package xyz.gaon.typoon.core.tile

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import xyz.gaon.typoon.MainActivity
import xyz.gaon.typoon.R

class ClipboardConvertTileService : TileService() {
    override fun onTileAdded() {
        updateTile()
    }

    override fun onStartListening() {
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            label = getString(R.string.qs_tile_label)
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                action = "xyz.gaon.typoon.action.CLIPBOARD_CONVERT"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        val activityWrapper =
            PendingIntentActivityWrapper(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
                false,
            )
        TileServiceCompat.startActivityAndCollapse(this, activityWrapper)
    }
}

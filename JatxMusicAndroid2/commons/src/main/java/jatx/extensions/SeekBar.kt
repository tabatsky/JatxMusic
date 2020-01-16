package jatx.extensions

import android.widget.SeekBar

fun SeekBar.onSeek(lambda: (Int) -> Unit) {
    this.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, i: Int, b: Boolean) {
            if (b) lambda(i)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}

        override fun onStopTrackingTouch(p0: SeekBar?) {}
    })
}
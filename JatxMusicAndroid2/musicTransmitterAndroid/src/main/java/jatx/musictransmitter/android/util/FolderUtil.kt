package jatx.musictransmitter.android.util

import java.io.File
import java.util.*

fun findFiles(startPath: String, pattern: String): List<File> {
    val dirs = arrayListOf<File>()
    val files = arrayListOf<File>()
    val startDir = File(startPath)
    if (!startDir.exists() || !startDir.isDirectory) {
        println("Wrong start path")
        return files
    }
    var dir = startDir
    var next = 0
    while (true) {
        val list = dir.listFiles()
        var i = 0
        while (list != null && i < list.size) {
            val tmp = list[i]
            if (tmp.isDirectory) {
                dirs.add(tmp)
            } else if (tmp.isFile && tmp.name.matches(Regex(pattern))) {
                files.add(tmp)
            }
            i++
        }
        if (next >= dirs.size) {
            break
        }
        dir = dirs[next]
        next++
    }
    return files
}
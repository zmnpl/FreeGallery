package com.labs.pbrother.freegallery.extension

import android.database.Cursor
import android.provider.MediaStore
import com.labs.pbrother.freegallery.controller.*

fun Cursor.makeImageItem(): Item {
    return Item(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
            getString(PATH),
            getLong(DATEADDED),
            getLong(DATETAKEN),
            getLong(SIZE),
            getInt(WIDTH),
            getInt(HEIGHT)
    )
}

fun Cursor.makeVideoItem(): Item {
    return Item(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
            getString(PATH),
            getLong(DATEADDED),
            getLong(DATETAKEN),
            getLong(SIZE),
            getInt(WIDTH),
            getInt(HEIGHT)
    )
}

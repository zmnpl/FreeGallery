package com.labs.pbrother.freegallery.controller

/**
 * Created by simon on 01.12.15.
 */
data class CollectionItem constructor(val id: String = "",
                                      var type: String = "",
                                      var thumb: String = "",
                                      var count: Int = 0,
                                      var color: Int = 0,
                                      var isLoved: Boolean = false
) : Comparable<CollectionItem> {

    val displayName: String = makeDisplayName()

    private fun makeDisplayName(): String {
        if (type === TYPE_FOLDER) {
            val parts = id.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return "../" + parts[parts.size - 1]
        }
        return id
    }

    val displayNameDetail: String
        get() = if (type === TYPE_TAG) {
            displayName
        } else nicePathNotation()


    private fun nicePathNotation(): String {
        val t = id.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var nicepath = "/"
        t.indices
                .asSequence()
                .filter { t[it].isNotEmpty() }
                .forEach {
                    if (it == t.size - 1) {
                        nicepath += t[it]
                    } else {
                        nicepath = nicepath + t[it].substring(0, 1) + "/"
                    }
                }
        return nicepath
    }

    /**
     * @return File url to thumbnail; default from first image in collection.
     */
    val thumbUrl: String
        get() = "file://" + thumb

    /**
     * Love this collection
     *
     * @param lv
     */
    fun love(lv: Boolean) {
        isLoved = lv
    }

    /**
     * Setter method
     *
     * @param colorId
     */
    fun colorize(colorId: Int) {
        this.color = colorId
    }

    /**
     * Sets objects meta from received CollectionMeta object
     */
    fun infuseMeta(meta: CollectionMeta) {
        this.colorize(meta.color)
        this.love(meta.loved)
    }

    override operator fun compareTo(other: CollectionItem): Int = sortRegular(other)

    private fun sortRegular(ccitem: CollectionItem): Int = when {
        isLoved && !ccitem.isLoved -> -1
        !isLoved && ccitem.isLoved -> 1
        displayName == ccitem.displayName -> -1
        else -> displayName.compareTo(ccitem.displayName)
    }
}



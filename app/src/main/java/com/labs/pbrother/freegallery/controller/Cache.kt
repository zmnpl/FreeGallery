package com.labs.pbrother.freegallery.controller

import java.util.ArrayList
import java.util.LinkedHashMap

/**
 * Created by simon on 30.11.17.
 */
object Cache {
    val overviewCache: LinkedHashMap<String, CollectionItem> = LinkedHashMap()
    val drawerCache: LinkedHashMap<String, CollectionItem> = LinkedHashMap()
    val itemCache: LinkedHashMap<String, ArrayList<Item>> = LinkedHashMap()
    val tagCache: HashSet<String> = HashSet()
}
